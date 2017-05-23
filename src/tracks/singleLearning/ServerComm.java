package tracks.singleLearning;

/**
 * Created by Daniel on 05.04.2017.
 */

import core.game.SerializableStateObservation;
import core.game.StateObservation;
import ontology.Types;

import java.io.*;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ServerComm {

    private static final Logger logger = Logger.getLogger(core.player.LearningPlayer.class.getName());

    /**
     * Reader of the player. Will read actions from the client.
     */
    public static BufferedReader input;

    /**
     * Writer of the player. Used to pass the client the state view information.
     */
    public static BufferedWriter output;

    /**
     * Line separator for messages.
     */
    private String lineSep = System.getProperty("line.separator");

    /**
     * Client process
     */
    private Process client;

    /**
     * Static block that handles the debug log creation.
     */
    static {
        FileHandler fh;
        try {
            // This block configure the logger with handler and formatter
            fh = new FileHandler("logs/serverCommDebug.txt");
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Public constructor of the player.
     * @param client process that runs the agent.
     */
    public ServerComm(Process client) {
        this.client = client;
        initBuffers();
    }

    /**
     * Creates the buffers for pipe communication.
     */
    public void initBuffers() {
        input = new BufferedReader(new InputStreamReader(client.getInputStream()));
        output = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
    }

    /***
     * This method is used to set the game state to either "ABORT_STATE" or "END_STATE"
     * depending on the termination of the game. Each game calls this method upon teardown
     * of the player object.
     *
     * END_STATE: Game ends normally
     * ABORT_STATE: Game is violently ended by player using "ABORT" message or ACTION_ESCAPE key
     *
     * @param so State observation of the game in progress to be used for message sending.
     * @return response by the client (level to be played)
     */
    public int finishGame(StateObservation so){

        try
        {
            // Set the game state to the appropriate state and the millisecond counter, then send the serialized observation.
            if(so.getAvatarLastAction() == Types.ACTIONS.ACTION_ESCAPE)
                so.currentGameState = Types.GAMESTATES.ABORT_STATE;
            else
                so.currentGameState = Types.GAMESTATES.END_STATE;

            SerializableStateObservation sso = new SerializableStateObservation(so);

            commSend(sso.serialize(null));

            String response = commRecv();


            if(response == null || response.equals("END_OVERSPENT"))
            {
                return -1;
            }

            if (response.matches("^[0-3]$")) {
                return Integer.parseInt(response);
            } else {
                return new Random().nextInt(3);
            }


        }catch(Exception e){
            System.out.println("Error sending results to the client:");
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Sends a message through the pipe.
     *
     * @param msg message to send.
     */
    public void commSend(String msg) throws IOException {
        output.write(msg + lineSep);
        output.flush();
    }

    /**
     * Receives a message from the client.
     *
     * @return the response got from the client, or null if no response was received after due time.
     */
    public String commRecv() throws IOException {
        String ret = input.readLine();
        while (ret != null && ret.trim().length() > 0) {// TODO: 22/05/17 if or while
            System.out.println(ret);
            return ret.trim();
        }
        return null;
    }

    /**
     * This function is called at the beginning of the game for
     * initialization.
     * Will give up if no "START_DONE" received after having received 11 responses
     */
    public boolean start() {
        try {
            int count = 11;
            commSend("START");
            String response;

            while(count>0) {
                response = commRecv();
                if (response==null) {
                    System.out.println("For tests: null response");
                    count--;
                } else if(response.equalsIgnoreCase("START_FAILED"))
                {
                    System.out.println("START_FAILED");
                    //Disqualification because of timeout.
                    return false;
                } else if (response.equalsIgnoreCase("START_DONE")) {
                    logger.fine("Received: " + response);
                    System.out.println("\nStart done");
                    return true;
                } else {
                    System.out.println("For tests: not START_DONE, not START_FAILED: "+response);
//                    response = commRecv();
                    count--;
                }
//                else if(count <= 0) {
//                    System.out.println("3");
//                    //Disqualification before too many things received that are not appropriate.
//                    System.out.println("Start failed: too many unexpected messages received");
//                    return false;
//                }  else {
//
//                }

            }

            if (count<=0) {
                System.out.println("Start failed: too many unexpected messages received");
                return false;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        //Disqualification because of exception, communication fail.
        System.out.println("Communication failed for unknown reason, could not play any games :-( ");
        return false;

    }

}

