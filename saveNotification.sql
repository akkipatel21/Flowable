CREATE DEFINER=`root`@`%` PROCEDURE `Save_Notifications`(IN notificationsIN JSON)
BEGIN
  -- Declare iterator variables to use them later in the loop
  DECLARE i INT DEFAULT 0;  -- notifications array
  DECLARE j INT DEFAULT 0;  -- users array
  
  -- Rollback if an error occurs
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    SHOW ERRORS;
    ROLLBACK;
  END;

  START TRANSACTION;

  -- Delete existing data from the NOTIFICATION table
  DELETE FROM NOTIFICATION;

  -- Extract the notifications array from the provided JSON
  SET @notifications = JSON_EXTRACT(notificationsIN, '$.notifications');
  SET @notifications_length = JSON_LENGTH(@notifications);

  WHILE i < @notifications_length DO
    -- Retrieve current notification from notifications array
    SET @notification = JSON_EXTRACT(@notifications, CONCAT('$[', i, ']'));
    SET @notification_Name = JSON_UNQUOTE(JSON_EXTRACT(@notification, '$.notificationName'));
    SET @template = JSON_UNQUOTE(JSON_EXTRACT(@notification, '$.template'));
    
    SET @userList = '';
    SET @userArray = JSON_EXTRACT(@notification, '$.users');
    SET @userArray_length = JSON_LENGTH(@userArray);
    SET j = 0; -- Reset the iterator variable

    WHILE j < @userArray_length DO
      SET @userValue = JSON_UNQUOTE(JSON_EXTRACT(@userArray, CONCAT('$[', j, ']')));
      SET @userList = CONCAT_WS(',', @userList, @userValue);
      SET j = j + 1; -- Increment the iterator variable
    END WHILE;
    SET @userList = TRIM(BOTH ',' FROM @userList);
    -- Insert into NOTIFICATION
    IF(@notification_Id IS NULL) THEN
		 -- get max id and add one to create new
        -- SET @Milestone_Id = (SELECT MAX(ID) FROM MILESTONE) + 1;
         SET @notification_Id = (SELECT COALESCE(MAX(ID), 0) + 1 FROM NOTIFICATION);
		 -- SELECT concat('Milestone_Id= ', @Milestone_Id); 
            
	 INSERT INTO NOTIFICATION (ID, NOTIFICATION_NAME, TEMPLATE, USERS)
	VALUES ( @notification_Id, @notification_Name, @template, @userList);
    ELSE
     INSERT INTO NOTIFICATION (ID, NOTIFICATION_NAME, TEMPLATE, USERS)
	VALUES ( @notification_Id, @notification_Name, @template, @userList);
    END IF; 
	
    
    SET i = i + 1; -- Increment the iterator variable
    SET @notification_Id = NULL; 
  END WHILE;
  
  COMMIT;
END
