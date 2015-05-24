package com.example.android.bluetoothchat;

/**
 * Created by Pamela Tomic on 5/23/2015.
 */
public class Question {

    private int id;
    private String ask;
    private int answer;
    private String difficulty;

    public Question() {}

    public Question(int newID, String newAsk, int newAnswer)
    {
        id = newID;
        ask = newAsk;
        answer = newAnswer;

    }

    public void setID(int newID)
    {
        id = newID;
    }

    public void setQuestion(String newAsk)
    {
        ask = newAsk;
    }

    public void setAnswer (int newAnswer)
    {
        answer = newAnswer;
    }


    public String getQuestion()
    {
        return ask;
    }

    public int getID()
    {
        return id;
    }

    public int getAnswer()
    {
        return answer;
    }



    public String toString(){
        return "ID: " + id + "   Question:" + ask;
    }

}


