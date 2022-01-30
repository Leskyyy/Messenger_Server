package put.sk.messenger_client.protocols;

public class Message {
    private String sender;
    private String receiver;
    private String content;
    private int totalLen;

    public Message(String sender, String receiver, String content){
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.totalLen = 4 + 4 + sender.length() + 4 + receiver.length() + 4 + content.length();
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getTotalLen() {
        return totalLen;
    }

    public void setTotalLen(int totalLen) {
        this.totalLen = totalLen;
    }

    @Override
    public String toString() {
        return "Message{" +
                "sender='" + sender + '\'' +
                ", receiver='" + receiver + '\'' +
                ", content='" + content + '\'' +
                ", totalLen=" + totalLen +
                '}';
    }
}
