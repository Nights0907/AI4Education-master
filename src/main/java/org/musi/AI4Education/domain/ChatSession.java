package org.musi.AI4Education.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ChatSession {
    private List<HashMap<String, String>> messages = new ArrayList<>(); // Store messages

    private String wholeDialog = "";

    public void addContent(String content){
        this.wholeDialog = this.wholeDialog + content;
    }

    public String getContent(){
        return this.wholeDialog;
    }

    public void ClearContent(){
        wholeDialog= "";

    }

    public void addMessage(HashMap<String, String> message) {
        messages.add(message);
    }

    public List<HashMap<String, String>> getMessages() {
        return messages;
    }

    public void clearMessages() {
        messages.clear();
    }
}
