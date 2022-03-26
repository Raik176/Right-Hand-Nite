package util;

import java.util.ArrayList;
import java.util.List;

public class Party {
    public String id;
    private List<PartyMember> members;
    private String partyLeader = "";

    public Party(String id) {
        this.id = id;
        this.members = new ArrayList<>();
    }

    public void addMember(String id) {
        members.add(new PartyMember(id, PartyMember.PartyRole.MEMBER));
    }
    public void removeMember(String id) {
        members.stream().filter(pm -> pm.id.equalsIgnoreCase(id)).forEach(pm -> members.remove(pm));
    }
    public List<PartyMember> getMembers() {
        return members;
    }
    public void setPartyLeader(String id) {
        members.stream().filter(m -> m.id.equalsIgnoreCase(getPartyLeader())).forEach(m -> {
            m.role = PartyMember.PartyRole.MEMBER;
        });
        members.stream().filter(m -> m.id.equalsIgnoreCase(id)).forEach(m -> {
            m.role = PartyMember.PartyRole.CAPTAIN;
        });
    }
    public String getPartyLeader() {
        return partyLeader;
    }
}
