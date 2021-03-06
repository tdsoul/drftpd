package org.drftpd.nukefilter.master;

/**
 * @author phew
 */
public class NukeFilterNukeConfig {

    private String nuker;
    private String exempts;

    public NukeFilterNukeConfig() {
        nuker = "drftpd";
        exempts = "";
    }

    public String getNuker() {
        return nuker;
    }

    public void setNuker(String nuker) {
        this.nuker = nuker;
    }

    public String getExempts() {
        return exempts;
    }

    public void setExempts(String exempts) {
        this.exempts = exempts;
    }

    public String[] getExemptsArray() {
        return exempts.split(";");
    }

}
