package org.huys.isight;

/**
 * User: huys03@hotmail.com
 * Date: 13-2-24
 * Time: 7:33pm
 * To modelling script component in Isight
 */
public class Script {
    private String language;
    private StringBuilder code = new StringBuilder();

    public String getLanguage() {
        return language;
    }

        public void setLanguage(String language) {
        this.language = language;
    }

    public void addLine(String line) {
        code.append(line + "\n");
    }

    public String toString() {
        return code.toString();
    }
}
