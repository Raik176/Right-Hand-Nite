package util;

import java.nio.file.Path;

public class XMPPManager extends Manager {
    public XMPPManager(Listener l2) {
        super(l2);
    }

    @Override
    public void listen(Listener l) {
        l.addListener("/client/bgs/:fileName", Listener.ListenerType.ALL,(req,res) -> res.send(Path.of("client/bgs/" + req.get("fileName").getAsString())));
    }
}
