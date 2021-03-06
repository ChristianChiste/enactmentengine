package at.enactmentengine.serverless.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Main class of enactment engine which specifies the input file and starts the
 * workflow on the machine on which it gets started.
 * <p>
 * based on @author markusmoosbrugger, jakobnoeckl
 * extended by @author stefanpedratscher
 */
public class Local {

    /**
     * Logger for the local execution.
     */
    static final Logger logger = LoggerFactory.getLogger(Local.class);

    /**
     * Starting point of the local execution.
     *
     * @param args workflow.yaml [input.json]
     */
    public static void main(String[] args) {

        /* Workflow executor */
        Executor executor = new Executor();

        /* Check for inputs and execute workflow */
        Map<String, Object> result = null;
        if (args.length > 1) {
            result = executor.executeWorkflow(args[0], args[1],  -1);
        } else if (args.length > 0) {
            result = executor.executeWorkflow(args[0], null,  -1);
        } else {
            logger.error("Usage: java -jar enactment-engine-all.jar path/to/workflow.yaml [path/to/input.json]");
        }

        logger.info("Result: {}", result);
    }
}
