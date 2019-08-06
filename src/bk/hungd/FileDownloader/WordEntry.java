/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bk.hungd.FileDownloader;

import java.util.ArrayList;

/**
 *
 * @author hungd
 */
public class WordEntry {

    private int id;
    private String word;
    private ArrayList<String> types;
    private String status;
    private String link;
    private String path;

    public WordEntry(String word, ArrayList<String> types) {
        this.word = word;
        this.types = types;
    }

    public WordEntry(int id, String word, ArrayList<String> types, String status, String link, String path) {
        this.id = id;
        this.word = word;
        this.types = types;
        this.status = status;
        this.link = link;
        this.path = path;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public ArrayList<String> getTypes() {
        return types;
    }

    public String getTypeString() {
        String sTypes = "";
        for (int i = 0; i < types.size() - 1; i++) {
            sTypes += types.get(i) + ", ";
        }
        if (types.size() > 0) {
            sTypes += types.get(types.size() - 1);
        }
        return sTypes;
    }

    public void setTypes(ArrayList<String> types) {
        this.types = types;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
    
    
}
