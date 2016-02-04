package de.tu_berlin.cit.intercloud.xmpp.client.service;

import de.tu_berlin.cit.rwx4j.XmppURI;
import de.tu_berlin.cit.rwx4j.rest.RestDocument;
import de.tu_berlin.cit.rwx4j.xwadl.XwadlDocument;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import java.io.IOException;
import java.util.List;

public interface IXmppService {
    // implementation is using the XmppConnectionManager

    void connect(XmppURI uri, String password) throws XMPPException, IOException, SmackException;
    void disconnect();

    List<String> discoverRestfulItems(XmppURI uri) throws XMPPException, IOException, SmackException;

    RestDocument sendRestDocument(XmppURI uri, RestDocument document) throws XMPPException, IOException, SmackException;
    XwadlDocument getXwadlDocument(XmppURI uri) throws XMPPException, IOException, SmackException;
}
