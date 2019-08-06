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
public class Functions {

    public static ArrayList<String> parseString(String src, char c) {
        ArrayList<String> results = new ArrayList<>();
        String s = src;
        if (s.indexOf(c) == -1) {
            results.add(s);
        } else {
            do {
                results.add(s.substring(0, s.indexOf(c) + 1));
                if (s.indexOf(c) < s.length()) {
                    s = s.substring(s.indexOf(c) + 1);
                } else {
                    s = "";
                }
                if (s.indexOf(c) == -1) {
                    results.add(s);
                }
            } while (s.indexOf(c) != -1);
        }

        return results;
    }

    public static ArrayList<String> parseString(String src, String c) {
        ArrayList<String> results = new ArrayList<>();
        String s = src;
        if (s.indexOf(c) == -1) {
            results.add(s);
        } else {
            do {
                results.add(s.substring(0, s.indexOf(c) + c.length()));
                if (s.indexOf(c) < s.length()) {
                    s = s.substring(s.indexOf(c) + c.length());
                } else {
                    s = "";
                }
                if (s.indexOf(c) == -1) {
                    results.add(s);
                }
            } while (s.indexOf(c) != -1);
        }

        return results;
    }

    private static String[] arr = {" n.", " v.", " adj.", " adv.", " pron.", " conj.",
        " excl.", " inf.", " det.", " def.", " prep.", " prefix.", " suffix.", " number.",
        " auxv.", " abbr.", " modv.", " linkv.", " comb."};

    public static WordEntry getWordEntry(String src) {
        String word = "";
        ArrayList<String> types = new ArrayList<>();

        String s = src;
        for (int i = 0; i < arr.length; i++) {
            if (s.lastIndexOf(arr[i]) != -1) {
                types.add(arr[i].trim());
                s = s.substring(0, s.lastIndexOf(arr[i]))+s.substring(s.lastIndexOf(arr[i])+arr[i].length());
            }
        }
        word = s;
        while(word.endsWith(",")){
            word = word.substring(0,word.length()-1);
        }

        return new WordEntry(word, types);
    }

    public static ArrayList<String> clean(ArrayList<String> arr) {
        ArrayList<String> results = new ArrayList<>(arr);
        for (int i = 0; i < results.size(); i++) {
            String s = results.get(i);
            if (s.endsWith(".,")) {
                results.set(i, s.substring(0, s.indexOf(".,")));
            }
            if (s.endsWith(".")) {
                results.set(i, s.substring(0, s.indexOf(".")));
            }
        }
        return results;
    }
}
