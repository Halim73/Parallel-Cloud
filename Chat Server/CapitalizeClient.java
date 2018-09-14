import com.sun.javafx.runtime.SystemProperties;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.lang.annotation.Target;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Random;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import javax.sound.sampled.*;
/**
 * A simple Swing-based client for the capitalization server.
 * It has a main frame window with a text field for entering
 * strings and a textarea to see the results of capitalizing
 * them.
 */
public class CapitalizeClient {

    private BufferedReader in;
    private PrintWriter out;
    private DataOutputStream os;
    private DataInputStream is;
    private JFrame frame = new JFrame("Capitalize Client");
    private JTextField dataField = new JTextField(40);
    private JTextArea messageArea = new JTextArea(10, 60);
    private Socket socket;
    private DatagramSocket datagramSocket;
    private TargetDataLine targetLine;
    private SourceDataLine source;
    private boolean recording = false;
    private boolean streaming = false;
    private String recipient;
    private String user;
    private String inet;
    private int port;
    /**
     * Constructs the client by laying out the GUI and registering a
     * listener with the textfield so that pressing Enter in the
     * listener sends the textfield contents to the server.
     */
    public CapitalizeClient() {

        // Layout GUI
        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JButton record = new JButton("record");
        JButton stop = new JButton("stop");
        JButton play = new JButton("play");
        JButton send = new JButton("send");

        southPanel.add(record);
        southPanel.add(stop);
        southPanel.add(play);
        southPanel.add(send);

        mainPanel.add(dataField, "North");
        mainPanel.add(new JScrollPane(messageArea), "Center");
        mainPanel.add(southPanel,"South");

        frame.add(mainPanel);

        messageArea.setEditable(false);
        DefaultCaret caret = (DefaultCaret)messageArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        // Add Listeners
        record.addActionListener(a-> {
            Runnable recordThread = ()->{
                recording = true;
                boolean toDo =  false;

                String name = user+"_testSound.wav";
                File file = new File(name);

                targetLine.start();

                AudioInputStream is = new AudioInputStream(targetLine);

                System.out.println("BEGIN RECORDING");
                while(recording){
                   toDo = (streaming)?stream(is):record(file,is);
                }
                System.out.println("RECORDING STOPPED "+!toDo);
            };
            Thread thread= new Thread(recordThread);
            thread.start();
        });
        stop.addActionListener(a-> {
            recording = false;
            targetLine.stop();
            targetLine.close();
        });
        play.addActionListener(a-> {
            String[] options = {"local file","received file"};
            int response = JOptionPane.showOptionDialog(null, "which file to play", "AudioFile",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                    null, options, options[0]);
            String file = (response == 0)?user+"_testSound.wav":user+"_received-testSound.wav";
            File audioFile = new File(file);
            play(audioFile);
        });
        send.addActionListener(a-> {
            recipient = JOptionPane.showInputDialog(
                    frame,
                    "Enter the name of the recipient",
                    JOptionPane.QUESTION_MESSAGE);

            String audioFile = user+"_testSound.wav";
            File file = new File(audioFile);

            out.println("%*file "+audioFile);
            out.println(file.length());
            out.println(recipient);

            System.out.println("getting ready to send file to "+recipient);
            try{

                byte[]buffer = new byte[(int)file.length()];
                BufferedInputStream fileStream = new BufferedInputStream(new FileInputStream(file));
                fileStream.read(buffer,0,buffer.length);

                os.write(buffer,0,buffer.length);
                System.out.println("FILE SEND SUCCESSFUL");
            }catch(Exception s){
                s.printStackTrace();
            }
        });
        dataField.addActionListener(new ActionListener() {
            /**
             * Responds to pressing the enter key in the textfield
             * by sending the contents of the text field to the
             * server and displaying the response from the server
             * in the text area.   If the response is "." we exit
             * the whole application, which closes all sockets,
             * streams and windows.
             */
            public void actionPerformed(ActionEvent e) {
                out.println(dataField.getText());
                dataField.selectAll();
            }
        });
    }

    /**
     * Implements the connection logic by prompting the end user for
     * the server's IP address, connecting, setting up streams, and
     * consuming the welcome messages from the server.   The Capitalizer
     * protocol says that the server sends three lines of text to the
     * client immediately after establishing a connection.
     */
    public void play(File file){
        try{
            AudioInputStream audio = AudioSystem.getAudioInputStream(file);

            source.start();

            byte[]buffer = new byte[4096];
            int read = -1;

            while((read = audio.read(buffer,0,buffer.length)) != -1){
                source.write(buffer,0,read);
            }
            source.drain();
            audio.close();
        }catch(Exception w){
            w.printStackTrace();
        }
    }
    public boolean stream(AudioInputStream is){
        int toWrite;

        byte[]buffer = new byte[targetLine.getBufferSize()/5];

        try{
            toWrite = is.read(buffer,0,buffer.length);
            if(toWrite > 0){
                os.write(buffer,0,buffer.length);
            }
        }catch (IOException i){
            i.printStackTrace();
        }
        return recording;
    }

    public boolean record(File file,AudioInputStream is){
        try{
            System.out.println("BEGIN RECORDING");
            AudioSystem.write(is,AudioFileFormat.Type.WAVE,file);
        }catch (IOException i){
            i.printStackTrace();
        }
        return recording;
    }
    public void connectToServer() throws IOException {

        // Get the server address from a dialog box.
        String serverAddress = JOptionPane.showInputDialog(
                frame,
                "Enter IP Address of the Server:",
                "Welcome to the Capitalization Program",
                JOptionPane.QUESTION_MESSAGE);

        // Make connection and initialize streams
        socket = new Socket(serverAddress, 9898);
        in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        os = new DataOutputStream(socket.getOutputStream());
        is = new DataInputStream(socket.getInputStream());
        // Consume the initial welcoming messages from the server
        for (int i = 0; i < 3; i++) {
            messageArea.append(in.readLine()+"\r\n");
        }
        user = in.readLine();

        Runnable readerThread = ()->{
            String response;
            while(true){
                try {
                    response = in.readLine();
                    if(recording)continue;

                    if (response == null || response.equals(".@kill")) {
                        System.out.println("client to terminate.");
                        socket.close();
                        System.exit(0);
                    }
                    if(response.equalsIgnoreCase("%stream")){
                        streaming = true;
                        inet = in.readLine();
                        port = Integer.parseInt(in.readLine());
                        if(datagramSocket == null){
                            datagramSocket = new DatagramSocket();
                        }
                        //out.println(datagramSocket.getLocalSocketAddress());
                        System.out.println("BEGINNING STREAMING SERVICES");
                    }
                    if(response.startsWith("[file")){
                        String name = user+"_received-testSound.wav";
                        File reply = new File(name);
                        FileOutputStream writer = new FileOutputStream(reply);

                        int length = Integer.parseInt(in.readLine());
                        byte[]buffer = new byte[length];
                        System.out.println("the size of the file is "+length);

                        is.readFully(buffer);
                        writer.write(buffer);
                        writer.close();

                        play(reply);
                    }
                } catch (IOException ex) {
                    response = "Error: " + ex;
                    System.out.println("" + response + "\n");
                    return;
                }
                messageArea.append(response + "\n");
            }
        };
        Thread thread = new Thread(readerThread);
        thread.start();
    }

    public void setVoiceServices(){
        try{
            AudioFormat format = new AudioFormat(44100,16,2,true,false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class,format);
            DataLine.Info tInfo = new DataLine.Info(TargetDataLine.class,format);

            source = (SourceDataLine)AudioSystem.getLine(info);
            targetLine = (TargetDataLine)AudioSystem.getLine(tInfo);
            source.open(format);
            targetLine.open(format);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    /**
     * Runs the client application.
     */
    public static void main(String[] args) throws Exception {
        CapitalizeClient client = new CapitalizeClient();
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.pack();
        client.frame.setVisible(true);
        client.connectToServer();
        client.setVoiceServices();
    }
}