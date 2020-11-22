package at.enactmentengine.serverless.parser;

import at.enactmentengine.serverless.nodes.ExecutableWorkflow;
import at.enactmentengine.serverless.nodes.ListPair;
import at.enactmentengine.serverless.nodes.Node;
import at.uibk.dps.afcl.utils.Utils;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;


/**
 * Class for parsing YAML files into an executable workflow.
 *
 * @author markusmoosbrugger, jakobnoeckl
 * extended by @author stefanpedratscher
 */
public class YAMLParser {

    static final Logger logger = LoggerFactory.getLogger(YAMLParser.class);
    static final String JSON_SCHEMA = "schema.json";

    /**
     * Parses a given YAML file to a workflow, which can be executed.
     *
     * @param filename yaml file to parse
     * @return Instance of class Executable workflow.
     */
    public ExecutableWorkflow parseExecutableWorkflow(byte[] filename, Language language, int executionId, Socket socket) {

        // Parse yaml file
        at.uibk.dps.afcl.Workflow workflow = null;

        if (language == Language.YAML) {
            try {
                workflow = Utils.readYAMLNoValidation(filename);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                return null;
            }
        } else if (language == Language.JSON) {
            throw new NotImplementedException("JSON file currently not supported.");
        } else {
            throw new NotImplementedException("Workflow language currently not supported.");
        }


        return getExecutableWorkflow(workflow, executionId, socket);
    }

    /**
     * Parses a given JSON string to a workflow, which can be executed.
     *
     * @param content JSON string to parse
     * @return Instance of class Executable workflow.
     */
    public ExecutableWorkflow parseExecutableWorkflowByStringContent(String content, Language language, int executionId) {

        // Parse yaml file
        at.uibk.dps.afcl.Workflow workflow = null;

        if (language == Language.YAML) {
            throw new NotImplementedException("YAML content currently not supported.");
        } else if (language == Language.JSON) {
            try {
                workflow = Utils.readJSONStringNoValidation(content);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                return null;
            }
        } else {
            throw new NotImplementedException("Workflow language currently not supported.");
        }

        return getExecutableWorkflow(workflow, executionId, null);
    }

    /**
     * Get an executable workflow
     *
     * @param workflow to convert
     * @return executable workflow
     */
    public ExecutableWorkflow getExecutableWorkflow(at.uibk.dps.afcl.Workflow workflow, int executionId, Socket socket) {

        ExecutableWorkflow executableWorkflow = null;
        if (workflow != null) {
            NodeListHelper nodeListHelper = new NodeListHelper();
            nodeListHelper.executionId = executionId;
            nodeListHelper.socket = socket; //for having the socket connection afterwards in every FunctionNode to communicate with scheduler

            // Create node pairs from workflow functions
            ListPair<Node, Node> workflowPair = new ListPair<>();
            ListPair<Node, Node> startNode = nodeListHelper.toNodeList(workflow.getWorkflowBody().get(0));
            workflowPair.setStart(startNode.getStart());
            Node currentEnd = startNode.getEnd();
            for (int i = 1; i < workflow.getWorkflowBody().size(); i++) {
                ListPair<Node, Node> current = nodeListHelper.toNodeList(workflow.getWorkflowBody().get(i));
                currentEnd.addChild(current.getStart());
                current.getStart().addParent(currentEnd);
                currentEnd = current.getEnd();
            }
            workflowPair.setEnd(currentEnd);

            // Create executable workflow from node pairs
            executableWorkflow = new ExecutableWorkflow(workflow.getName(), workflowPair, workflow.getDataIns());

            logger.info("Workflow was converted to an executable workflow.");
        }

        return executableWorkflow;
    }
}