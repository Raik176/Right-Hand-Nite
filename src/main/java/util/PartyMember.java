package util;

public class PartyMember {
    public enum PartyRole {
        MEMBER,
        CAPTAIN
    }
    public String id;
    public PartyRole role;

    public PartyMember(String id, PartyRole role) {
        this.id = id;
        this.role = role;
    }
}
