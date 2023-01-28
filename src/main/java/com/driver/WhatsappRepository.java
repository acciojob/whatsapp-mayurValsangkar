package com.driver;

import java.sql.Timestamp;
import java.util.*;

import org.springframework.stereotype.Repository;

@Repository
public class WhatsappRepository {

    //Assume that each user belongs to at most one group
    //You can use the below mentioned hashmaps or delete these and create your own.
    private HashMap<Group, List<User>> groupUserMap;
    private HashMap<Group, List<Message>> groupMessageMap;
    private HashMap<Message, User> senderMap;
    private HashMap<Group, User> adminMap;
    private HashSet<String> userMobile;
    private int customGroupCount;
    private int messageId;

    public WhatsappRepository(){
        this.groupMessageMap = new HashMap<Group, List<Message>>();
        this.groupUserMap = new HashMap<Group, List<User>>();
        this.senderMap = new HashMap<Message, User>();
        this.adminMap = new HashMap<Group, User>();
        this.userMobile = new HashSet<>();
        this.customGroupCount = 0;
        this.messageId = 0;
    }

    public String createUser(String name, String mobile) throws Exception {

        if(userMobile.contains(mobile)){
            throw new Exception("User already exists");
        }

        User user = new User(name, mobile);
        userMobile.add(mobile);
        return "SUCCESS";
    }

    public Group createGroup(List<User> users){

        String groupName = "";
        if(users.size()>2){
            this.customGroupCount++;
            groupName = "Group "+this.customGroupCount;
        }else{
            groupName = users.get(1).getName();
        }

        Group group = new Group(groupName, users.size());
        groupUserMap.put(group, users);
        adminMap.put(group, users.get(0));
        groupMessageMap.put(group, new ArrayList<Message>());

        return group;
    }

    public int createMessage(String content){
        this.messageId++;
        Message message = new Message(messageId, content);
        return message.getId();
    }

    public int sendMessage(Message message, User sender, Group group) throws Exception {

       if(!groupUserMap.containsKey(group)) throw new Exception("Group does not exist");

       boolean isMember = false;
       List<User> userList = groupUserMap.get(group);
       for(User user : userList){
           if(user.equals(sender)) {
               isMember = true;
               break;
           }
       }

       if(isMember==false) throw new Exception("You are not allowed to send message");

       List<Message> messageList = new ArrayList<>();
       if(groupMessageMap.containsKey(group)) messageList = groupMessageMap.get(group);

       messageList.add(message);
       groupMessageMap.put(group, messageList);
       return messageList.size();
    }

    public String changeAdmin(User approver, User user, Group group) throws Exception{

        if(!groupUserMap.containsKey(group)) throw new Exception("Group does not exist");
        if(!adminMap.get(group).equals(approver)) throw new Exception("Approver does not have rights");

        boolean isMember = false;
        List<User> userList = groupUserMap.get(group);
        for(User user1 : userList){
            if(user1.equals(user)) {
                isMember = true;
                break;
            }
        }

        if(isMember==false) throw new Exception("User is not a participant");

        adminMap.put(group, user);
        return "SUCCESS";
    }

    public int removeUser(User user) throws Exception{
        boolean userFound = false;
        Group userGroup = null;
        for(Group group : groupUserMap.keySet()){
            if(groupUserMap.get(group).contains(user)){
                if(Objects.equals(adminMap.get(userGroup).getName(), user.getName())){
                    throw new Exception("Cannot remove admin");
                }
                userGroup = group;
                userFound = true;
                break;
            }
        }

        if(userFound==false){
            throw new Exception("User not found");
        }

        List<User> userList = groupUserMap.get(userGroup);
        //List<User> updatedUserList = new ArrayList<>();

        for(User user1 : userList){
            if(user1.equals(user)){
                userList.remove(user);
                break;
            }
        }
        groupUserMap.put(userGroup, userList);

        List<Message> messageList = groupMessageMap.get(userGroup);
        for(Message message : messageList){
            if(senderMap.get(message).equals(user)){
                messageList.remove(message);
                break;
            }
        }
        groupMessageMap.put(userGroup, messageList);

        for(Message message : senderMap.keySet()){
            if(senderMap.get(message).equals(user)){
                senderMap.remove(user);
                break;
            }
        }

        return groupUserMap.get(userGroup).size() + groupMessageMap.get(userGroup).size() + senderMap.size();
    }

    public String findMessage(Date start, Date end, int K) throws Exception{

        List<Message> messageList = new ArrayList<>();
        for(Group group : groupUserMap.keySet()){
            messageList.addAll(groupMessageMap.get(group));
        }

        List<Message> filteredMessageList = new ArrayList<>();
        for(Message message : messageList){
            if(message.getTimestamp().after(start) && message.getTimestamp().before(end)){
                filteredMessageList.add(message);
            }
        }

        if(filteredMessageList.size()< K){
            throw new Exception("K is greater than the number of messages");
        }

        Collections.sort(filteredMessageList, new Comparator<Message>() {
            @Override
            public int compare(Message m1, Message m2) {
                return m2.getTimestamp().compareTo(m1.getTimestamp());
            }
        });

        return filteredMessageList.get(K-1).getContent();
    }
}
