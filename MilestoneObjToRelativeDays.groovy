ObjectMapper mapper = new ObjectMapper();
JsonNode milestoneNode = mapper.readTree(milestoneObj);

ArrayNode milestonesArray = (ArrayNode) milestoneNode.get(0);
log.info("@@@@@@@  milestonesArray " + milestonesArray);
List<ObjectNode> milestoneList = new ArrayList<>();
List<IDTaskIdentifier> idTaskIdentifierList = new ArrayList<>();

class IDTaskIdentifier {
    private String id;
    private String taskIdentifier;

    public IDTaskIdentifier(String id, String taskIdentifier) {
        this.id = id;
        this.taskIdentifier = taskIdentifier;
    }

    public String getId() {
        return id;
    }

    public String getTaskIdentifier() {
        return taskIdentifier;
    }
}

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
        JsonNode prevMilestone;

        if (predecessorNames.length > 1) {
            prevMilestone = milestonesArray.stream()
                .filter(prev -> predecessorName.equals(prev.get("MILESTONE_NAME").asText()) && "INACTIVE".equals(prev.get("STATUS").asText()) && "RELATIVE".equals(prev.get("TYPE").asText()))
                .findFirst()
                .orElse(null);
        } else {
            prevMilestone = milestonesArray.stream()
                .filter(prev -> predecessorName.equals(prev.get("MILESTONE_NAME").asText()) && "RELATIVE".equals(prev.get("TYPE").asText()))
                .findFirst()
                .orElse(null);
        }

        if (prevMilestone != null) {
            int prevDuration = prevMilestone.get("DURATION").asInt();
            int prevDaysExtension = prevMilestone.get("DAYS_EXTENSION").asInt();
            relativeDays += prevDuration + prevDaysExtension;
        }
    }
}


    milestoneObject.put("relativeDays", relativeDays);
    milestoneList.add(milestoneObject);

    if (relativeDays > 30 && "ACTIVE".equals(status)) {
        String id = milestoneObject.get("ID").asText();
        String taskIdentifier = milestoneObject.get("TASK_IDENTIFIER").asText();
        idTaskIdentifierList.add(new IDTaskIdentifier(id, taskIdentifier));
    }
}
log.info("@@@@@@@  milestoneList " + milestoneList);
//log.info("@@@@@@@  milestoneList " + milestoneList);
for (IDTaskIdentifier item : idTaskIdentifierList) {
    String id = item.getId();
    String taskIdentifier = item.getTaskIdentifier();
    // Perform any operations with the ID and Task Identifier values
    log.info("ID: " + id + ", Task Identifier: " + taskIdentifier);
}

