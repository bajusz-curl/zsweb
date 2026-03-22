package zsweb;

import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;
import javax.microedition.io.*;
import javax.microedition.media.*;
import java.io.*;
import java.util.Vector;

public class ZsWebBrowser extends MIDlet implements CommandListener {

    private Display display;

    private Form main;
    private TextField urlField;
    private StringItem content;

    private List linksList, mediaList, historyList, bookmarksList;

    private Vector links = new Vector();
    private Vector media = new Vector();
    private Vector history = new Vector();
    private Vector bookmarks = new Vector();

    private Command go = new Command("Go", Command.OK, 1);
    private Command linksCmd = new Command("Links", Command.SCREEN, 2);
    private Command mediaCmd = new Command("Media", Command.SCREEN, 3);
    private Command historyCmd = new Command("History", Command.SCREEN, 4);
    private Command bookmarkCmd = new Command("Bookmark", Command.SCREEN, 5);
    private Command bookmarksCmd = new Command("Bookmarks", Command.SCREEN, 6);
    private Command back = new Command("Back", Command.BACK, 1);
    private Command exit = new Command("Exit", Command.EXIT, 9);

    private static final String HTTPS = "https://zsweb.onrender.com";
    private static final String HTTP  = "http://10.0.2.2:3000";

    public ZsWebBrowser() {

        display = Display.getDisplay(this);

        main = new Form("zsweb");
        urlField = new TextField("URL", "https://example.com", 255, TextField.URL);
        content = new StringItem("", "Welcome to zsweb ULTRA");

        main.append(urlField);
        main.append(content);

        main.addCommand(go);
        main.addCommand(linksCmd);
        main.addCommand(mediaCmd);
        main.addCommand(historyCmd);
        main.addCommand(bookmarkCmd);
        main.addCommand(bookmarksCmd);
        main.addCommand(exit);

        main.setCommandListener(this);

        linksList = new List("Links", List.IMPLICIT);
        mediaList = new List("Media", List.IMPLICIT);
        historyList = new List("History", List.IMPLICIT);
        bookmarksList = new List("Bookmarks", List.IMPLICIT);

        linksList.addCommand(back);
        mediaList.addCommand(back);
        historyList.addCommand(back);
        bookmarksList.addCommand(back);

        linksList.setCommandListener(this);
        mediaList.setCommandListener(this);
        historyList.setCommandListener(this);
        bookmarksList.setCommandListener(this);
    }

    public void startApp() {
        display.setCurrent(main);
    }

    public void pauseApp() {}
    public void destroyApp(boolean u) {}

    public void commandAction(Command c, Displayable d) {

        if (d == main) {

            if (c == go) load(urlField.getString());

            if (c == linksCmd) display.setCurrent(linksList);
            if (c == mediaCmd) display.setCurrent(mediaList);
            if (c == historyCmd) display.setCurrent(historyList);
            if (c == bookmarksCmd) display.setCurrent(bookmarksList);

            if (c == bookmarkCmd) {
                String u = urlField.getString();
                bookmarks.addElement(u);
                bookmarksList.append(u, null);
            }

            if (c == exit) notifyDestroyed();
        }

        else if (d == linksList) {
            if (c == List.SELECT_COMMAND) openFromList(links, linksList);
            if (c == back) display.setCurrent(main);
        }

        else if (d == mediaList) {
            if (c == List.SELECT_COMMAND) playFromList();
            if (c == back) display.setCurrent(main);
        }

        else if (d == historyList) {
            if (c == List.SELECT_COMMAND) openFromList(history, historyList);
            if (c == back) display.setCurrent(main);
        }

        else if (d == bookmarksList) {
            if (c == List.SELECT_COMMAND) openFromList(bookmarks, bookmarksList);
            if (c == back) display.setCurrent(main);
        }
    }

    private void openFromList(Vector vec, List list) {
        int i = list.getSelectedIndex();
        if (i >= 0) {
            String u = (String) vec.elementAt(i);
            urlField.setString(u);
            load(u);
        }
    }

    private void load(final String url) {

        if (url == null || url.length() == 0) return;

        content.setText("Loading...");

        history.addElement(url);
        historyList.append(url, null);

        new Thread(new Runnable() {
            public void run() {
                if (!fetch(HTTPS, url)) {
                    if (!fetch(HTTP, url)) {
                        show("Connection failed");
                    }
                }
            }
        }).start();
    }

    private boolean fetch(String base, String url) {

        HttpConnection hc = null;
        InputStream is = null;

        try {
            String full = base + "/render?url=" + encode(url);

            hc = (HttpConnection) Connector.open(full);
            hc.setRequestMethod(HttpConnection.GET);

            is = hc.openInputStream();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int ch;

            while ((ch = is.read()) != -1) {
                bos.write(ch);
            }

            parse(new String(bos.toByteArray(), "UTF-8"));
            return true;

        } catch (Exception e) {
            return false;
        }

        finally {
            try { if (is != null) is.close(); } catch (Exception e) {}
            try { if (hc != null) hc.close(); } catch (Exception e) {}
        }
    }

    private void parse(final String data) {

        links.removeAllElements();
        media.removeAllElements();

        linksList.deleteAll();
        mediaList.deleteAll();

        if (data == null || data.length() == 0) {
            show("Empty page");
            return;
        }

        String[] lines = split(data, '\n');
        String body = "";

        for (int i = 0; i < lines.length; i++) {

            String line = lines[i];

            if (line.startsWith("TITLE|")) {
                main.setTitle(line.substring(6));
            }

            else if (line.startsWith("BODY|")) {
                body += line.substring(5) + "\n";
            }

            else if (line.startsWith("LINK|")) {
                String l = line.substring(5);
                links.addElement(l);
                linksList.append(l, null);
            }

            else if (line.startsWith("MEDIA|")) {
                String m = line.substring(6);
                media.addElement(m);
                mediaList.append(m, null);
            }
        }

        final String f = body;

        display.callSerially(new Runnable() {
            public void run() {
                content.setText(f.length() > 0 ? f : "No content");
            }
        });
    }

    private void playFromList() {
        int i = mediaList.getSelectedIndex();
        if (i < 0) return;

        try {
            String url = (String) media.elementAt(i);
            Player p = Manager.createPlayer(url);
            p.realize();
            p.start();
        } catch (Exception e) {
            show("Media error");
        }
    }

    private void show(final String msg) {
        display.callSerially(new Runnable() {
            public void run() {
                content.setText(msg);
            }
        });
    }

    private String encode(String s) {
        StringBuffer out = new StringBuffer();

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if ("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.~:/?&=#".indexOf(c) != -1) {
                out.append(c);
            } else if (c == ' ') {
                out.append('+');
            }
        }
        return out.toString();
    }

    private String[] split(String s, char sep) {
        Vector v = new Vector();
        int p = 0;

        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == sep) {
                v.addElement(s.substring(p, i));
                p = i + 1;
            }
        }

        v.addElement(s.substring(p));

        String[] arr = new String[v.size()];
        v.copyInto(arr);
        return arr;
    }
}
