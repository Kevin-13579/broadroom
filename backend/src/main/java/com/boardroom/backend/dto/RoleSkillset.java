package com.boardroom.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RoleSkillset {

    private String role;
    private String focus;
    private List<String> skills = new ArrayList<>();
    private List<String> responsibilities = new ArrayList<>();

    @JsonProperty("dos")
    private List<String> dos = new ArrayList<>();

    @JsonProperty("donts")
    private List<String> donts = new ArrayList<>();

    public RoleSkillset() {
    }

    public RoleSkillset(String role, String focus, List<String> skills, List<String> responsibilities, List<String> dos, List<String> donts) {
        this.role = role;
        this.focus = focus;
        this.skills = skills != null ? skills : new ArrayList<>();
        this.responsibilities = responsibilities != null ? responsibilities : new ArrayList<>();
        this.dos = dos != null ? dos : new ArrayList<>();
        this.donts = donts != null ? donts : new ArrayList<>();
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getFocus() {
        return focus;
    }

    public void setFocus(String focus) {
        this.focus = focus;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills != null ? skills : new ArrayList<>();
    }

    public List<String> getResponsibilities() {
        return responsibilities;
    }

    public void setResponsibilities(List<String> responsibilities) {
        this.responsibilities = responsibilities != null ? responsibilities : new ArrayList<>();
    }

    public List<String> getDos() {
        return dos;
    }

    public void setDos(List<String> dos) {
        this.dos = dos != null ? dos : new ArrayList<>();
    }

    public List<String> getDonts() {
        return donts;
    }

    public void setDonts(List<String> donts) {
        this.donts = donts != null ? donts : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "RoleSkillset{" +
                "role='" + role + '\'' +
                ", focus='" + focus + '\'' +
                ", skills=" + skills +
                ", responsibilities=" + responsibilities +
                ", dos=" + dos +
                ", donts=" + donts +
                '}';
    }
}
