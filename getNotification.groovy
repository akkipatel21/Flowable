import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

// Get current milestoneDay
def milestoneDay = execution.getVariable("milestoneDay")
def dayValue = milestoneDay.getData()
def currentMilestoneDay = dayValue.day.asInt()
println "currentMilestoneDay: $currentMilestoneDay"

// Get the response ArrayList from the execution variable
def restMilestoneObj = execution.getVariable("milestoneObj")
def restNotificationObj = execution.getVariable("notificationObj")

// Convert the ArrayList to a Groovy object using ObjectMapper
def mapper = new ObjectMapper()
ArrayNode milestoneArrayNode = mapper.convertValue(restMilestoneObj, ArrayNode)
ArrayNode notificationArrayNode = mapper.convertValue(restNotificationObj, ArrayNode)

// Access the "milestones" array from the first element of the milestoneArrayNode
JsonNode milestonesNode = milestoneArrayNode.get(0).get("data").get("milestones")
JsonNode notificationsNode = notificationArrayNode.get(0).get("data").get("notifications")

// Now you have milestoneArrayNode and notificationArrayNode as ArrayNode, and milestonesNode and notificationsNode as JsonNode.

println "MILESTONES JsonNode: $milestonesNode"
println "NOTIFICATIONS JsonNode: $notificationsNode"

// Create a List<ObjectNode> to store the milestones with the "relativeDays" field
List<ObjectNode> milestoneList = new ArrayList<>();

// Iterate through each milestone in the milestonesNode
milestonesNode.forEach { milestone ->
    
    def milestoneStatus = milestone.get("STATUS").asInt()
    def milestoneType = milestone.get("TYPE").asText()
    
    // Check if the milestone has STATUS=1 and TYPE="ABSOLUTE"
    if (milestoneStatus == 1 && milestoneType == "ABSOLUTE") {
        processMilestone(milestone, milestonesNode, notificationsNode, currentMilestoneDay, milestoneList)
    } else if (milestoneStatus == 1 && milestoneType == "RELATIVE") {
        processMilestoneWithPredecessors(milestone, milestonesNode, notificationsNode, currentMilestoneDay, milestoneList)
    }
}

// Function to process milestone without predecessors
def processMilestone(milestone, milestonesNode, notificationsNode, currentMilestoneDay, milestoneList) {
    // Clone the milestone as ObjectNode to add the "relativeDays" field
    ObjectNode milestoneWithRelativeDays = milestone.deepCopy()

    // Get the "STARTED_ON" value from the milestone and calculate the "relativeDays"
    def duration = milestone.get("STARTED_ON").asInt()
    def startedOn = milestone.get("DURATION").asInt()
    def daysExtension = milestone.get("DAYS_EXTENSION").asInt()
    def relativeDays = duration + daysExtension + startedOn

    if (relativeDays > currentMilestoneDay) {
        // Get the "ID" value from the milestone
        def milestoneId = milestone.get("ID").asInt()

        // Find matching notifications for the milestone
        List<JsonNode> matchingNotifications = findMatchingNotifications(notificationsNode, milestoneId, relativeDays - currentMilestoneDay)
        
        // Print the notifications for the milestone that matches the condition
        println "Notifications for Milestone ID $milestoneId: $matchingNotifications"

        // Add the "relativeDays" field to the milestone object
        milestoneWithRelativeDays.put("relativeDays", relativeDays)

        // Add the milestone with "relativeDays" to the milestoneList
        milestoneList.add(milestoneWithRelativeDays)
    }
}

// Function to process milestone with predecessors
def processMilestoneWithPredecessors(milestone, milestonesNode, notificationsNode, currentMilestoneDay, milestoneList) {
    // Clone the milestone as ObjectNode to add the "relativeDays" field
    ObjectNode milestoneWithRelativeDays = milestone.deepCopy()

    // Get the "DURATION" and "DAYS_EXTENSION" values from the milestone
    def duration = milestone.get("DURATION").asInt()
    def daysExtension = milestone.get("DAYS_EXTENSION").asInt()

    // Calculate the "relativeDays" using duration and daysExtension of the current milestone
    def relativeDays = duration + daysExtension

    // Get the "PREDECESSOR" value from the milestone
    def predecessor = milestone.get("PREDECESSOR").asText()

    if (predecessor != null && !predecessor.isEmpty()) {
        // Split the predecessor names and iterate through them
        String[] predecessorNames = predecessor.split(",")
        for (String pred : predecessorNames) {
            String predecessorName = pred.trim()

            // Find the predecessor milestone in the milestonesNode
            JsonNode predecessorMilestone = milestonesNode.find { node ->
                node.get("MILESTONE_NAME").asText() == predecessorName && node.get("STATUS").asInt() == 1
            }

            if (predecessorMilestone != null) {
                // Get the "DURATION" and "DAYS_EXTENSION" values from the predecessor milestone
                int prevDuration = predecessorMilestone.get("DURATION").asInt()
                int prevDaysExtension = predecessorMilestone.get("DAYS_EXTENSION").asInt()
                // Add predecessor's duration and daysExtension to the "relativeDays"
                relativeDays += prevDuration + prevDaysExtension
            }
        }
    }
    
    // Find matching notifications for the milestone
    List<JsonNode> matchingNotifications = findMatchingNotifications(notificationsNode, milestone.get("ID").asInt(), relativeDays - currentMilestoneDay)

    // Print the notifications for the milestone that matches the condition
    println "Notifications for Milestone ID ${milestone.get("ID").asInt()}: $matchingNotifications"

    // Add the "relativeDays" field to the milestone object
    milestoneWithRelativeDays.put("relativeDays", relativeDays)

    // Add the milestone with "relativeDays" to the milestoneList
    milestoneList.add(milestoneWithRelativeDays)
}

// Function to find matching notifications for a milestone
def findMatchingNotifications(notificationsNode, milestoneId, daysDifference) {
    List<JsonNode> matchingNotifications = new ArrayList<>();
    notificationsNode.forEach { notification ->
        if (notification.get("MILESTONE_ID").asInt() == milestoneId) {
            def notifyBeforeDays = notification.get("NOTIFY_BEFORE_DAYS").asInt()
            if (daysDifference == notifyBeforeDays) {
                matchingNotifications.add(notification)
            }
        }
    }
    return matchingNotifications
}

// Print the milestoneList with "relativeDays" field
println "MILESTONE List with relativeDays: $milestoneList"
