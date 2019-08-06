/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bk.hungd.FileDownloader;

import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author hungd
 */
public class MainFrame extends javax.swing.JFrame {

    /**
     * Variables
     */
    private final String defaultLogFile = "FileDownloader.log";
    private String defaultSrcPath;
    private String defaultXmlPath;

    private DefaultTableModel model;
    private ArrayList<WordEntry> arrWords;

    private Downloader downloader;
    private boolean stopDownloader;
    private boolean pauseDownloader;
    private int nextDownloadID;

    /**
     * Creates new form MainFrame
     */
    public MainFrame() {
        initComponents();
        this.setLocationRelativeTo(null);

        defaultSrcPath = "";
        defaultXmlPath = "";

        configPopupMenu(tfInput, pmInput);
        configPopupMenu(tfOutput, pmOutput);
        configPopupMenu(taUrls, pmUrls);
        configPopupMenu();

        readLogFile();

        table.setAutoCreateRowSorter(true);
        model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        arrWords = new ArrayList<>();

        stopDownloader = false;
        pauseDownloader = false;
        nextDownloadID = 0;
    }

    private ArrayList<String> getDefaultUrls() {
        ArrayList<String> urls = new ArrayList<>();

        urls.add("http://audio.oxforddictionaries.com/en/mp3/word_us_1.mp3");
        urls.add("http://audio.oxforddictionaries.com/en/mp3/word_us_2.mp3");
        urls.add("http://audio.oxforddictionaries.com/en/mp3/word_us_3.mp3");
        urls.add("http://audio.oxforddictionaries.com/en/mp3/word_gb_1.mp3");
        urls.add("http://audio.oxforddictionaries.com/en/mp3/word_gb_2.mp3");
        urls.add("http://audio.oxforddictionaries.com/en/mp3/word_gb_3.mp3");

        return urls;
    }

    public void showLog(String s) {
        System.out.println("" + s);
    }

    public void setStatus(String s) {
        lbStatus.setText(s);
    }

    /**
     * Read previous information from log file
     */
    private void readLogFile() {
        showLog("READING information from log file...");
        new SwingWorker<ArrayList<String>, String>() {
            @Override
            protected ArrayList<String> doInBackground() throws Exception {
                publish("Reading log...");
                try {
                    File logFile = new File(System.getProperty("user.home") + File.separator + defaultLogFile);
                    ArrayList<String> results = new ArrayList<>();
                    if (logFile.exists() && logFile.isFile() && logFile.canRead()) {
                        FileReader fr = new FileReader(logFile);
                        BufferedReader br = new BufferedReader(fr);
                        String line = "";
                        while ((line = br.readLine()) != null) {
                            results.add(line);
                        }
                        fr.close();
                        return results;
                    } else {
                        return null;
                    }
                } catch (IOException ex) {
                    return null;
                }
            }

            @Override
            protected void process(List<String> chunks) {
                setStatus(chunks.get(0));
            }

            @Override
            protected void done() {
                try {
                    ArrayList<String> results = get();
                    if (results != null) {
                        for (int i = 0; i < results.size(); i++) {
                            String s = results.get(i);
                            if (s.startsWith(LogDefines.LOG_INPUT)) {
                                tfInput.setText(s.substring(s.indexOf(LogDefines.LOG_INPUT) + LogDefines.LOG_INPUT.length()));
                                defaultSrcPath = tfInput.getText();
                            }
                            if (s.startsWith(LogDefines.LOG_OUTPUT)) {
                                tfOutput.setText(s.substring(s.indexOf(LogDefines.LOG_OUTPUT) + LogDefines.LOG_OUTPUT.length()));
                                defaultXmlPath = tfOutput.getText();
                            }
                            if (s.startsWith(LogDefines.LOG_URL)) {
                                String url = s.substring(s.indexOf(LogDefines.LOG_URL) + LogDefines.LOG_URL.length());
                                cbbUrls.addItem(url);
                                taUrls.append(url + "\n");
                            }
                        }

                        if (cbbUrls.getItemCount() == 0) {
                            ArrayList<String> urls = getDefaultUrls();
                            for (int i = 0; i < urls.size(); i++) {
                                cbbUrls.addItem(urls.get(i));
                            }
                        }
                        showLog(" > Done reading log file!");
                        setStatus("Loading completed!");
                    } else {
                        ArrayList<String> urls = getDefaultUrls();
                        for (int i = 0; i < urls.size(); i++) {
                            cbbUrls.addItem(urls.get(i));
                        }
                        showLog(" > ERROR: Fail to read from log file!");
                        setStatus("");
                    }
                } catch (InterruptedException ex) {
                    showLog(" > ERROR: Fail to read from log file! " + ex.getMessage());
                    Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ExecutionException ex) {
                    showLog(" > ERROR: Fail to read from log file! " + ex.getMessage());
                    Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }.execute();
    }

    /**
     * Save current information to the log file
     */
    private void writeLogFile() {
        try {
            showLog("\nWRITING information to log file...");

            File logFile = new File(System.getProperty("user.home") + File.separator + defaultLogFile);
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            FileWriter fw = new FileWriter(logFile, false);
            String contents = "";

            String input = LogDefines.LOG_INPUT + tfInput.getText() + "\n";
            showLog(" > Input: " + tfInput.getText());
            String output = LogDefines.LOG_OUTPUT + tfOutput.getText() + "\n";
            showLog(" > Output: " + tfOutput.getText());
            contents += input + output;

            for (int i = 0; i < cbbUrls.getItemCount(); i++) {
                String url = LogDefines.LOG_URL + cbbUrls.getItemAt(i).toString() + "\n";
                showLog(" > Url: " + cbbUrls.getItemAt(i).toString());
                contents += url;
            }

            fw.write(contents);
            fw.close();
        } catch (IOException ex) {
            showLog(" > ERROR: Fail to write to log file! " + ex.getMessage());
            Logger
                    .getLogger(MainFrame.class
                            .getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Get system clipboard
     *
     * @return
     */
    private Clipboard getSystemClipboard() {
        Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
        Clipboard systemClipboard = defaultToolkit.getSystemClipboard();
        return systemClipboard;
    }

    /**
     * Configure popup menu for textField
     *
     * @param textField
     * @param popupMenu
     */
    private void configPopupMenu(JTextField textField, JPopupMenu popupMenu) {
        JMenuItem miCut = new JMenuItem(" Cut", new ImageIcon(getClass().getClassLoader().getResource("resources/images/cut.png")));
        miCut.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String fullText = textField.getText();
                String selectedText = textField.getSelectedText();
                if (selectedText != null && !selectedText.equalsIgnoreCase("")) {
                    getSystemClipboard().setContents(new StringSelection(selectedText), null);
                    int curPosition = fullText.indexOf(selectedText);
                    textField.setText(fullText.substring(0, fullText.indexOf(selectedText)) + ""
                            + fullText.substring(fullText.indexOf(selectedText) + selectedText.length()));
                    textField.setCaretPosition(curPosition);
                } else {
                    if (!fullText.equalsIgnoreCase("")) {
                        textField.setText("");
                        getSystemClipboard().setContents(new StringSelection(fullText), null);
                    }
                }
            }
        });
        popupMenu.add(miCut);

        JMenuItem miCopy = new JMenuItem(" Copy", new ImageIcon(getClass().getClassLoader().getResource("resources/images/copy.png")));
        miCopy.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String fullText = textField.getText();
                String selectedText = textField.getSelectedText();
                if (selectedText != null && !selectedText.equalsIgnoreCase("")) {
                    getSystemClipboard().setContents(new StringSelection(selectedText), null);
                } else {
                    if (!fullText.equalsIgnoreCase("")) {
                        getSystemClipboard().setContents(new StringSelection(fullText), null);
                    }
                }
            }
        });
        popupMenu.add(miCopy);

        JMenuItem miPaste = new JMenuItem(" Paste", new ImageIcon(getClass().getClassLoader().getResource("resources/images/paste.png")));
        miPaste.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String fullText = textField.getText();
                    String selectedText = textField.getSelectedText();

                    Transferable trans = getSystemClipboard().getContents(null);
                    StringSelection nowData = new StringSelection("");
                    if (!trans.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                        getSystemClipboard().setContents(nowData, nowData);
                    }
                    String pastedText = (String) getSystemClipboard().getContents(null).getTransferData(DataFlavor.stringFlavor);
                    int curPosition = textField.getCaretPosition();
                    if (pastedText != null) {
                        if (selectedText != null && !selectedText.equalsIgnoreCase("")) {
                            curPosition = curPosition - selectedText.length() + pastedText.length();
                            fullText = fullText.substring(0, fullText.indexOf(selectedText)) + pastedText
                                    + fullText.substring(fullText.indexOf(selectedText) + selectedText.length());
                        } else {
                            String lastText = "";
                            if (curPosition < fullText.length()) {
                                lastText = fullText.substring(curPosition + 1);
                            }
                            fullText = fullText.substring(0, curPosition) + pastedText + lastText;
                            curPosition = curPosition + pastedText.length();
                        }
                        textField.setText(fullText);
                        textField.setCaretPosition(curPosition);
                    }
                    textField.requestFocus();

                } catch (UnsupportedFlavorException ex) {
                    Logger.getLogger(MainFrame.class
                            .getName()).log(Level.SEVERE, null, ex);
                    showLog(ex.toString());

                } catch (IOException ex) {
                    Logger.getLogger(MainFrame.class
                            .getName()).log(Level.SEVERE, null, ex);
                    showLog(ex.toString());
                }
            }
        });
        popupMenu.add(miPaste);

        textField.setComponentPopupMenu(popupMenu);
    }

    /**
     * Configure popup menu for Logs textfield
     */
    private void configPopupMenu(JTextArea textArea, JPopupMenu popupMenu) {
        JMenuItem miClear = new JMenuItem(" Clear", new ImageIcon(getClass().getClassLoader().getResource("resources/images/clear.png")));
        miClear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                textArea.setText("");
            }
        });
        popupMenu.add(miClear);
        popupMenu.addSeparator();

        JMenuItem miCopy = new JMenuItem(" Copy", new ImageIcon(getClass().getClassLoader().getResource("resources/images/copy.png")));
        miCopy.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String fullText = textArea.getText();
                String selectedText = textArea.getSelectedText();
                if (selectedText != null && !selectedText.equalsIgnoreCase("")) {
                    getSystemClipboard().setContents(new StringSelection(selectedText), null);
                } else {
                    if (!fullText.equalsIgnoreCase("")) {
                        getSystemClipboard().setContents(new StringSelection(fullText), null);
                    }
                }
            }
        });
        popupMenu.add(miCopy);

        JMenuItem miReset = new JMenuItem(" Reset", new ImageIcon(getClass().getClassLoader().getResource("resources/images/select_all.png")));
        miReset.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                textArea.setText("");
                ArrayList<String> urls = getDefaultUrls();
                for (int i = 0; i < urls.size(); i++) {
                    textArea.append(urls.get(i) + "\n");
                }
                textArea.requestFocus();
                textArea.selectAll();
                getSystemClipboard().setContents(new StringSelection(textArea.getText()), null);
            }
        });
        popupMenu.add(miReset);
        textArea.setComponentPopupMenu(popupMenu);
    }

    private void configPopupMenu() {
        JMenuItem miDownload = new JMenuItem(" Download", new ImageIcon(getClass().getClassLoader().getResource("resources/images/clear.png")));
        miDownload.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] rows = table.getSelectedRows();
                for (int i = 0; i < rows.length; i++) {
                    int id = Integer.parseInt(table.getValueAt(rows[i], 0).toString());
                    String word = arrWords.get(id).getWord();
                    String sFile = tfOutput.getText() + File.separator + word + ".mp3";
                    downloadWord(id, word, sFile, false);
                    table.setRowSelectionInterval(rows[i], rows[i]);
                    table.scrollRectToVisible(new Rectangle(table.getCellRect(rows[i], 0, true)));
                }
            }
        });
        pmTable.add(miDownload);

        table.setComponentPopupMenu(pmTable);
    }

    /**
     * Check and insert an item to top of an array
     *
     * @param arr
     * @param item
     * @return
     */
    private ArrayList<String> checkAndAdd(ArrayList<String> arr, String item) {
        ArrayList<String> results = new ArrayList<>();
        if (!item.equals("")) {
            results.add(item);
        }
        for (int i = 0; i < arr.size(); i++) {
            if (!arr.get(i).equals(item)) {
                results.add(arr.get(i));
            }
        }
        return results;
    }

    /**
     * Check and remove a item in an array
     *
     * @param arr
     * @param item
     * @return
     */
    private ArrayList<String> checkAndRemove(ArrayList<String> arr, String item) {
        ArrayList<String> results = arr;
        for (int i = 0; i < arr.size(); i++) {
            if (results.get(i).equals(item)) {
                results.remove(i);
            }
        }
        return results;
    }

    private void createModel() {
        model.setRowCount(0);
        for (int i = 0; i < arrWords.size(); i++) {
            String[] data = new String[6];

            int id = arrWords.get(i).getId();
            String sID = "";
            if (id < 10) {
                sID = "000" + id;
            } else if (id < 100) {
                sID = "00" + id;
            } else if (id < 1000) {
                sID = "0" + id;
            } else {
                sID = "" + id;
            }

            data[0] = sID;
            data[1] = arrWords.get(i).getWord() + "";
            data[2] = arrWords.get(i).getTypeString() + "";
            data[3] = arrWords.get(i).getStatus() + "";
            data[4] = arrWords.get(i).getLink() + "";
            data[5] = arrWords.get(i).getPath() + "";

            model.addRow(data);
            table.setModel(model);
            model.fireTableDataChanged();
            table.convertRowIndexToModel(0);
        }
    }

    private void loadFile(File file) {
        showLog("LOADING data from input file...");
        new SwingWorker<ArrayList<String>, String>() {
            @Override
            protected ArrayList<String> doInBackground() throws Exception {
                publish("Loading ...");
                try {
                    ArrayList<String> results = new ArrayList<>();
                    if (file.exists() && file.isFile() && file.canRead()) {
                        FileReader fr = new FileReader(file);
                        BufferedReader br = new BufferedReader(fr);
                        String line = "";
                        while ((line = br.readLine()) != null) {
                            results.add(line);
                        }
                        fr.close();
                        return results;
                    } else {
                        return null;
                    }
                } catch (IOException ex) {
                    return null;
                }
            }

            @Override
            protected void process(List<String> chunks) {
                setStatus(chunks.get(0));
            }

            @Override
            protected void done() {
                try {
                    ArrayList<String> results = get();
                    if (results != null) {
                        arrWords.clear();
                        for (int i = 0; i < results.size(); i++) {
                            String s = results.get(i);
                            if (!s.equals("")) {
                                WordEntry word = Functions.getWordEntry(s);
                                arrWords.add(new WordEntry(i, word.getWord(), word.getTypes(), "Undownload", "null", "null"));
                            }
                        }

                        System.out.println(" > Total words: " + arrWords.size());
                        createModel();

                        for(int i=0; i<arrWords.size(); i++){
                            System.out.println(""+arrWords.get(i).getWord());
                        }
                        
                        for(int i=0; i<arrWords.size(); i++){
                            System.out.println(""+arrWords.get(i).getTypes());
                        }
                        
                        showLog(" > Done loading data!");
                        setStatus("Loading completed!");
                    } else {
                        showLog(" > ERROR: Fail to load data!");
                        setStatus("");
                    }
                } catch (InterruptedException ex) {
                    showLog(" > ERROR: Fail to load data! " + ex.getMessage());
                    Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ExecutionException ex) {
                    showLog(" > ERROR: Fail to load data! " + ex.getMessage());
                    Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }.execute();
    }

    private void refreshStatus(File file) {
        showLog("REFRESHING status from log file...");
        new SwingWorker<Hashtable<String, String>, String>() {
            @Override
            protected Hashtable<String, String> doInBackground() throws Exception {
                if (file.exists() && file.isDirectory()) {
                    Hashtable<String, String> results = new Hashtable<>();
                    File[] files = file.listFiles();
                    for (int i = 0; i < files.length; i++) {
                        String fileName = files[i].getName();
                        if (fileName.endsWith(".mp3")) {
                            results.put(fileName.substring(0, fileName.indexOf(".mp3")), files[i].getPath());
                        }
                        publish("Refreshing... " + fileName);
                    }
                    return results;
                } else {
                    return null;
                }
            }

            @Override
            protected void process(List<String> chunks) {
                lbStatus.setText(chunks.get(chunks.size() - 1));
            }

            @Override
            protected void done() {
                try {
                    Hashtable<String, String> results = get();
                    if (results != null) {
                        for (int i = 0; i < arrWords.size(); i++) {
                            WordEntry wordEntry =arrWords.get(i);
                            if (results.containsKey(wordEntry.getWord().toLowerCase())) {
                                arrWords.get(i).setStatus("OK");
                                arrWords.get(i).setPath(results.get(wordEntry.getWord().toLowerCase()));
                            } else {
                                arrWords.get(i).setStatus("Undownloaded");
                                arrWords.get(i).setPath("null");
                            }
                        }
                        createModel();
                    }
                    showLog(" > DONE refreshing status!");
                } catch (InterruptedException ex) {
                    Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ExecutionException ex) {
                    Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }.execute();
    }

    private void refreshURL(File file) {
        showLog("REFRESHING URLs from log file...");
        new SwingWorker<Hashtable<String, String>, String>() {
            @Override
            protected Hashtable<String, String> doInBackground() throws Exception {
                try {
                    if (file.exists() && file.isFile() && file.canRead()) {
                        Hashtable<String, String> results = new Hashtable<>();
                        FileReader fr = new FileReader(file);
                        BufferedReader br = new BufferedReader(fr);
                        String line = "";
                        while ((line = br.readLine()) != null) {
                            if (line.indexOf('@') != -1) {
                                String word = line.substring(0, line.indexOf('@'));
                                String url = line.substring(line.indexOf('@') + 1);
                                results.put(word, url);
                            }
                            publish(line);
                        }
                        fr.close();
                        return results;
                    } else {
                        return null;
                    }
                } catch (IOException ex) {
                    return null;
                }
            }

            @Override
            protected void process(List<String> chunks) {
                lbStatus.setText("Refreshing... " + chunks.get(chunks.size() - 1));
            }

            @Override
            protected void done() {
                try {
                    Hashtable<String, String> results = get();
                    if (results != null) {
                        for (int i = 0; i < arrWords.size(); i++) {
                            if (results.containsKey(arrWords.get(i).getWord())) {
                                arrWords.get(i).setLink(results.get(arrWords.get(i).getWord()));
                            }
                        }
                        createModel();
                    }
                    showLog(" > DONE refreshing URLs!");
                } catch (InterruptedException ex) {
                    Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ExecutionException ex) {
                    Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }.execute();
    }

    private boolean download(String sFile, String sURL) {
        try {
            File file = new File(sFile);
            URL url = new URL(sURL);
            FileUtils.copyURLToFile(url, file);
            return true;
        } catch (MalformedURLException ex) {
        } catch (IOException ex) {
        }
        return false;
    }

    private void downloadWord(int id, String word, String sFile, boolean all) {
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                boolean result = false;
                for (int j = 0; j < cbbUrls.getItemCount(); j++) {
                    String sUrl = cbbUrls.getItemAt(j);
                    sUrl = sUrl.replaceAll("word", word);

                    result = download(sFile, sUrl);
                    arrWords.get(id).setLink(sUrl);
                    arrWords.get(id).setPath(sFile);
                    if (result) {
                        arrWords.get(id).setStatus("OK");
                        break;
                    } else {
                        arrWords.get(id).setStatus("ERROR");
                    }
                }
                return result;
            }

            @Override
            protected void done() {
                try {
                    boolean result = get();
                    model.setValueAt(arrWords.get(id).getStatus(), id, 3);
                    model.setValueAt(arrWords.get(id).getLink(), id, 4);
                    model.setValueAt(arrWords.get(id).getPath(), id, 5);

                    model.fireTableDataChanged();
                    table.setRowSelectionInterval(id, id);
                    table.scrollRectToVisible(new Rectangle(table.getCellRect(id, 0, true)));
                    if (!all) {
                        lbStatus.setText("Downloaded: " + arrWords.get(id).getWord());
                    } else {
                        lbStatus.setText("Downloading (" + (100 * (id + 1) / arrWords.size()) + " %)... " + arrWords.get(id).getWord());
                    }
                    if (!result) {
                        System.out.println(" > Fail: " + arrWords.get(id).getWord());
                    }
                } catch (InterruptedException ex) {
                    Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ExecutionException ex) {
                    Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }.execute();
    }

    private void fastDownloader() {
        for (int i = 0; i < arrWords.size(); i++) {
            if (!arrWords.get(i).getStatus().equalsIgnoreCase("OK")) {
                String word = arrWords.get(i).getWord().toLowerCase();
                String sFile = tfOutput.getText() + File.separator + word.toLowerCase() + ".mp3";
                downloadWord(i, word, sFile, true);
            }
        }
    }

    class Downloader extends SwingWorker<Void, WordEntry> {

        @Override
        protected Void doInBackground() throws Exception {
            for (int i = nextDownloadID; i < arrWords.size(); i++) {
                String word = arrWords.get(i).getWord();
                String sFile = tfOutput.getText() + File.separator + word + ".mp3";

                boolean result = false;
                for (int j = 0; j < cbbUrls.getItemCount(); j++) {
                    String sUrl = cbbUrls.getItemAt(j);
                    sUrl = sUrl.replaceAll("word", word);

                    result = download(sFile, sUrl);
                    arrWords.get(i).setLink(sUrl);
                    arrWords.get(i).setPath(sFile);
                    if (result) {
                        arrWords.get(i).setStatus("OK");
                        break;
                    } else {
                        arrWords.get(i).setStatus("ERROR");
                    }
                }
                publish(arrWords.get(i));

                if (stopDownloader) {
                    break;
                }
                if (pauseDownloader) {
                    nextDownloadID = i + 1;
                    break;
                }
            }
            return null;
        }

        @Override
        protected void process(List<WordEntry> words) {
            for (int i = 0; i < words.size(); i++) {
                int id = words.get(i).getId();
                model.setValueAt(words.get(i).getStatus(), id, 3);
                model.setValueAt(words.get(i).getLink(), id, 4);
                model.setValueAt(words.get(i).getPath(), id, 5);
            }
            int lastID = words.get(words.size() - 1).getId();
            model.fireTableDataChanged();
            table.setRowSelectionInterval(lastID, lastID);
            table.scrollRectToVisible(new Rectangle(table.getCellRect(lastID, 0, true)));
            lbStatus.setText("Downloading (" + (100 * (lastID + 1) / arrWords.size()) + " %)... " + words.get(words.size() - 1).getWord());
        }

        @Override
        protected void done() {
            try {
                get();
                lbStatus.setText("Completed!");
            } catch (InterruptedException ex) {
                Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pmInput = new javax.swing.JPopupMenu();
        pmOutput = new javax.swing.JPopupMenu();
        pmUrls = new javax.swing.JPopupMenu();
        pmTable = new javax.swing.JPopupMenu();
        jLabel1 = new javax.swing.JLabel();
        tfInput = new javax.swing.JTextField();
        btOpen = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        tfOutput = new javax.swing.JTextField();
        btSave = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        lbStatus = new javax.swing.JLabel();
        btExit = new javax.swing.JButton();
        cbbUrls = new javax.swing.JComboBox<>();
        btStart = new javax.swing.JButton();
        btLoad = new javax.swing.JButton();
        btStop = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        spTable = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        spUrls = new javax.swing.JScrollPane();
        taUrls = new javax.swing.JTextArea();
        btUrls = new javax.swing.JToggleButton();
        btLogs = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("File Downloader");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jLabel1.setText("Input");

        btOpen.setText("Open");
        btOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btOpenActionPerformed(evt);
            }
        });

        jLabel2.setText("Output");

        btSave.setText("Save");
        btSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btSaveActionPerformed(evt);
            }
        });

        jLabel3.setText("URLs");

        lbStatus.setText("Status");

        btExit.setText("Exit");
        btExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btExitActionPerformed(evt);
            }
        });

        cbbUrls.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbbUrlsActionPerformed(evt);
            }
        });

        btStart.setText("Start");
        btStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btStartActionPerformed(evt);
            }
        });

        btLoad.setText("Load");
        btLoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btLoadActionPerformed(evt);
            }
        });

        btStop.setText("Stop");
        btStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btStopActionPerformed(evt);
            }
        });

        jPanel1.setLayout(new java.awt.CardLayout());

        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "ID", "Word", "Types", "Status", "Link", "Path"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, true, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        spTable.setViewportView(table);

        jPanel1.add(spTable, "card2");

        taUrls.setColumns(20);
        taUrls.setRows(5);
        spUrls.setViewportView(taUrls);

        jPanel1.add(spUrls, "card3");

        btUrls.setText("Edit...");
        btUrls.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btUrlsActionPerformed(evt);
            }
        });

        btLogs.setText("Logs");
        btLogs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btLogsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 548, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lbStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btLogs)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btLoad)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btStart)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btStop)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btExit, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel1)
                            .addComponent(jLabel3))
                        .addGap(8, 8, 8)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(tfOutput)
                            .addComponent(tfInput)
                            .addComponent(cbbUrls, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(btOpen, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btSave, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btUrls, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {btOpen, btSave, btUrls});

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel1, jLabel2});

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {btExit, btLoad, btLogs, btStart, btStop});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(tfInput, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btOpen, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(tfOutput, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btSave, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(cbbUrls, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btUrls))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 202, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(btExit, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lbStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btStart, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(btLoad, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(btStop, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(btLogs)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {btExit, btLoad, btLogs, btStart, btStop});

        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {btOpen, btSave, btUrls});

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btExitActionPerformed
        writeLogFile();
        System.exit(0);
    }//GEN-LAST:event_btExitActionPerformed

    private void btOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btOpenActionPerformed
        JFileChooser fileChooser = new JFileChooser(new File(defaultSrcPath));
        fileChooser.setDialogTitle("Select a file");
        int returnVal = fileChooser.showOpenDialog(MainFrame.this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File inFile = fileChooser.getSelectedFile();
            tfInput.setText(inFile.getPath());

            loadFile(inFile);
        }
    }//GEN-LAST:event_btOpenActionPerformed

    private void btSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btSaveActionPerformed
        JFileChooser fileChooser = new JFileChooser(new File(defaultXmlPath));
        fileChooser.setDialogTitle("Select a container folder");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int returnVal = fileChooser.showOpenDialog(MainFrame.this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File outFile = fileChooser.getSelectedFile();
            tfOutput.setText(outFile.getPath());
        }
    }//GEN-LAST:event_btSaveActionPerformed

    private void btStartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btStartActionPerformed
        fastDownloader();

//        if (btStart.getText() == "Start") {
//            btStart.setText("Pause");
//            stopDownloader = false;
//            if (!pauseDownloader) {
//                nextDownloadID = 0;
//            }
//            pauseDownloader = false;
//
//            downloader = new Downloader();
//            downloader.execute();
//        } else if (btStart.getText() == "Pause") {
//            btStart.setText("Start");
//            pauseDownloader = true;
//        }
    }//GEN-LAST:event_btStartActionPerformed

    private void btLoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btLoadActionPerformed
        File inFile = new File(tfInput.getText());
        loadFile(inFile);

        File outFile = new File(tfOutput.getText());
        refreshStatus(outFile);

        File urlFile = new File(tfOutput.getText() + File.separator + "urls.log");
        refreshURL(urlFile);
    }//GEN-LAST:event_btLoadActionPerformed

    private void cbbUrlsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbbUrlsActionPerformed
        showLog(" > Selected = " + cbbUrls.getSelectedIndex());
//        arrUrls = checkAndAdd(arrUrls, arrUrls.get(cbbUrls.getSelectedIndex()));
//        mCbbUrls.refresh(arrUrls);
    }//GEN-LAST:event_cbbUrlsActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        writeLogFile();
        System.exit(0);
    }//GEN-LAST:event_formWindowClosing

    private void btStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btStopActionPerformed
//        downloader.cancel(true);

        stopDownloader = true;
        pauseDownloader = false;
        nextDownloadID = 0;
        btStart.setText("Start");
    }//GEN-LAST:event_btStopActionPerformed

    private void btUrlsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btUrlsActionPerformed
        JToggleButton button = (JToggleButton) evt.getSource();

        if (button.isSelected()) {
            btUrls.setText("Refresh");
            spTable.setVisible(false);
            spUrls.setVisible(true);
        } else {
            btUrls.setText("Edit...");
            spTable.setVisible(true);
            spUrls.setVisible(false);

            String urls = taUrls.getText();
            cbbUrls.removeAllItems();
            while (urls.trim().indexOf('\n') != -1) {
                String url = urls.substring(0, urls.indexOf('\n'));
                if (!url.trim().equals("")) {
                    cbbUrls.addItem(url.trim());
                }
                urls = urls.substring(urls.indexOf('\n') + 1);
            }
            if (!urls.equals("")) {
                cbbUrls.addItem(urls);
            }
        }
    }//GEN-LAST:event_btUrlsActionPerformed

    private void btLogsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btLogsActionPerformed
        File urlFile = new File(tfOutput.getText() + File.separator + "urls.log");
        try {
            if (!urlFile.exists()) {
                urlFile.createNewFile();
            }
            FileWriter fw = new FileWriter(urlFile, false);
            for (int i = 0; i < arrWords.size(); i++) {
                WordEntry wordEntry = arrWords.get(i);
                if (wordEntry.getStatus().equalsIgnoreCase("OK")) {
                    fw.write(wordEntry.getWord() + "@" + wordEntry.getLink() + "\n");
                }
            }
            fw.close();
        } catch (IOException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_btLogsActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;

                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(MainFrame.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);

        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(MainFrame.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);

        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(MainFrame.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);

        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MainFrame.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new MainFrame().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btExit;
    private javax.swing.JButton btLoad;
    private javax.swing.JButton btLogs;
    private javax.swing.JButton btOpen;
    private javax.swing.JButton btSave;
    private javax.swing.JButton btStart;
    private javax.swing.JButton btStop;
    private javax.swing.JToggleButton btUrls;
    private javax.swing.JComboBox<String> cbbUrls;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel lbStatus;
    private javax.swing.JPopupMenu pmInput;
    private javax.swing.JPopupMenu pmOutput;
    private javax.swing.JPopupMenu pmTable;
    private javax.swing.JPopupMenu pmUrls;
    private javax.swing.JScrollPane spTable;
    private javax.swing.JScrollPane spUrls;
    private javax.swing.JTextArea taUrls;
    private javax.swing.JTable table;
    private javax.swing.JTextField tfInput;
    private javax.swing.JTextField tfOutput;
    // End of variables declaration//GEN-END:variables
}
