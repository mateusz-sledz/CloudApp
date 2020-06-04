package com.socket;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class Parser {

    String received;

    Parser(String message){
        this.received = message;
    }

    public String getBody(){
        String[] data = received.split("\r\n\r\n");

        System.out.println("received parser: " + received);

        if(data.length > 1 && data[1] != null){
            return data[1];
        }
        return "";
    }

    public String getHeaders(){

        String[] x = received.split("\r\n\r\n");

        return x[0];
    }

    public String[] getDirs(){

        Gson jsonParser = new Gson();

        System.out.println("RECEIVED PARSER: " + received);

        return jsonParser.fromJson(getBody(), String[].class);
    }

    public String get_status_code(){

        String[] headers = getHeaders().split("\r\n");
        String[] status = headers[0].split(" ");
        return status[1];
    }

    public String getHeaderValue(String header){
        String headers = getHeaders();

        String[] HeaderLines = headers.split("\r\n");

        String foundLine = "";

        for(String t : HeaderLines){
            if(t.contains(header)){
                foundLine = t;
                break;
            }
        }

        if(foundLine.equals("")){
            System.out.println("Header not found");
            return "";
        }

        String[] data = foundLine.split(": ");

        return data[1];

    }

    public String get_content_range(String line){
        System.out.println("line: " + line);
        String[] range_data = line.split(" ");
        String[] values = range_data[1].split("/");
        String[] to_return = values[0].split("-");
        List<String> list = new ArrayList<String>();
        list.add(to_return[0]);
        list.add(to_return[1]);
        list.add(values[1]);

        int result = Integer.parseInt(values[1]);

        return  values[1];                     // to_return contains content range values
    }                               // eg. Content-Range: bytes 200-1000/67589
                                    // to_return[0, 1, 2] = 200, 1000, 67589

    public void setmessage(String newmessage){
        this.received = newmessage;
    }
}
