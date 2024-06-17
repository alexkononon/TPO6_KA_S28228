package zad1.main.java;

import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

public class ChatClient extends JFrame implements MessageListener {
    private final String clientId;
    private JTextArea messageArea;
    private JTextField inputField;
    private Session session;
    private MessageProducer producer;

    public ChatClient(String clientId) {
        this.clientId = clientId;
        initComponents();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    String clientId = JOptionPane.showInputDialog("Your client ID:");
                    ChatClient chatClient = new ChatClient(clientId);
                    chatClient.start();
                    chatClient.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void initComponents() {
        setTitle("Client number " + clientId);
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        messageArea = new JTextArea();
        messageArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(messageArea);

        inputField = new JTextField();
        inputField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    sendMessage(inputField.getText());
                    inputField.setText("");
                } catch (JMSException jmsException) {
                    jmsException.printStackTrace();
                }
            }
        });

        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    sendMessage(inputField.getText());
                    inputField.setText("");
                } catch (JMSException jmsException) {
                    jmsException.printStackTrace();
                }
            }
        });

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        add(inputField, BorderLayout.SOUTH);
        add(sendButton, BorderLayout.EAST);
    }

    public void start() throws NamingException, JMSException {
        Hashtable<String, String> env = new Hashtable<>(11);
        env.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
        String brokerURL = "tcp://localhost:61616";
        env.put(Context.PROVIDER_URL, brokerURL);
        env.put("queue.myQueue", "myQueue");

        Context ctx = new InitialContext(env);
        ActiveMQConnectionFactory connectionFactory = (ActiveMQConnectionFactory) ctx.lookup("ConnectionFactory");

        Connection connection = connectionFactory.createConnection();
        connection.setClientID(clientId);
        connection.start();

        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Topic topic = session.createTopic("chat");

        MessageConsumer consumer = session.createConsumer(topic);
        consumer.setMessageListener(this);

        producer = session.createProducer(topic);
        producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
    }

    private void sendMessage(String text) throws JMSException {
        TextMessage message = session.createTextMessage();
        message.setText(clientId + ": " + text);
        producer.send(message);
    }

    @Override
    public void onMessage(Message message) {
        if (message instanceof TextMessage) {
            try {
                TextMessage textMessage = (TextMessage) message;
                String text = textMessage.getText();
                messageArea.append(text + "\n");
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }
    }
}
