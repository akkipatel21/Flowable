import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.ArrayList;

Logger log = LoggerFactory.getLogger(getClass());

def milestoneObj = '[{"day":2,"Milestone":[{"ID":1,"MILESTONE_NAME":"Milestone 1","CASE_ID":"Case123","TASK_IDENTIFIER":"Task 1","DURATION":5,"STATUS":"ACTIVE","DAYS_EXTENSION":0,"REMAINING_DAY":0,"ORIGINAL_TIME":5,"PREDECESSOR":null,"TYPE":"ABSOLUTE"},{"ID":2,"MILESTONE_NAME":"Milestone 2","CASE_ID":"Case123","TASK_IDENTIFIER":"Task 2","DURATION":30,"STATUS":"ACTIVE","DAYS_EXTENSION":0,"REMAINING_DAY":0,"ORIGINAL_TIME":30,"PREDECESSOR":"Milestone 1 ,Milestone 2","TYPE":"RELATIVE"},{"ID":3,"MILESTONE_NAME":"Milestone 3","CASE_ID":"Case123","TASK_IDENTIFIER":"Task 3","DURATION":60,"STATUS":"INACTIVE","DAYS_EXTENSION":0,"REMAINING_DAY":0,"ORIGINAL_TIME":60,"PREDECESSOR":"Milestone 2","TYPE":"RELATIVE"},{"ID":4,"MILESTONE_NAME":"Milestone 4","CASE_ID":"Case123","TASK_IDENTIFIER":"Task 4","DURATION":20,"STATUS":"ACTIVE","DAYS_EXTENSION":0,"REMAINING_DAY":0,"ORIGINAL_TIME":20,"PREDECESSOR":"Milestone 3","TYPE":"RELATIVE"},{"ID":5,"MILESTONE_NAME":"Milestone 5","CASE_ID":"Case123","TASK_IDENTIFIER":"Task 5","DURATION":10,"STATUS":"ACTIVE","DAYS_EXTENSION":0,"REMAINING_DAY":0,"ORIGINAL_TIME":10,"PREDECESSOR":null,"TYPE":"RELATIVE"}]}]'

ObjectMapper mapper = new ObjectMapper();
JsonNode milestoneNode = mapper.readTree(milestoneObj);
ArrayNode milestonesArray = (ArrayNode) milestoneNode.get(0).get("Milestone");
//what if it is in Array
List<ObjectNode> milestoneList = new ArrayList<>();

for (JsonNode milestone : milestonesArray) {
    ObjectNode milestoneObject = (ObjectNode) milestone;
    milestoneObject.remove("day"); // Remove the "day" field from each milestone object

    int duration = milestoneObject.get("DURATION").asInt();
    int daysExtension = milestoneObject.get("DAYS_EXTENSION").asInt();
    String predecessor = milestoneObject.get("PREDECESSOR").asText();
    String status = milestoneObject.get("STATUS").asText();

    int relativeDays = duration + daysExtension;

    if (predecessor != null && !predecessor.isEmpty()) {
        String[] predecessorNames = predecessor.split(",");
        for (String pred : predecessorNames) {
            String predecessorName = pred.trim();
            for (JsonNode prevMilestone : milestonesArray) {
                String prevMilestoneName = prevMilestone.get("MILESTONE_NAME").asText();
                String prevMilestoneType = prevMilestone.get("TYPE").asText();
                String prevMilestoneStatus = prevMilestone.get("STATUS").asText();
                
                if (predecessorName.equals(prevMilestoneName) && "RELATIVE".equals(prevMilestoneType) && "INACTIVE".equals(prevMilestoneStatus)) {
                    int prevDuration = prevMilestone.get("DURATION").asInt();
                    int prevDaysExtension = prevMilestone.get("DAYS_EXTENSION").asInt();
                    relativeDays += prevDuration + prevDaysExtension;
                    break;
                }
            }
        }
    }

    milestoneObject.put("relativeDays", relativeDays);
    milestoneList.add(milestoneObject);
}

List<ObjectNode> filteredMilestones = milestoneList.stream()
    .filter(milestone -> milestone.get("relativeDays").asInt() > 30 && "ACTIVE".equals(milestone.get("STATUS").asText()))
    .toList();

List<String> idTaskIdentifierList = filteredMilestones.stream()
    .map(milestone -> "ID: " + milestone.get("ID").asText() + ", Task_Identifier: " + milestone.get("TASK_IDENTIFIER").asText())
    .toList();

log.info("@@@ ID and Task_Identifier List = " + idTaskIdentifierList);
