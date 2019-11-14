package at.enactmentengine.serverless.nodes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.enactmentengine.serverless.exception.MissingInputDataException;
import at.enactmentengine.serverless.exception.NoSwitchCaseFulfilledException;
import at.enactmentengine.serverless.model.Case;
import at.enactmentengine.serverless.model.Data;

/**
 * Control node which manages the tasks at the start of a switch element.
 * 
 * @author markusmoosbrugger, jakobnoeckl
 *
 */
public class SwitchStartNodeOld extends Node {
	private List<Data> dataIns;
	private List<Case> cases;
	private Data dataEval;
	final static Logger logger = LoggerFactory.getLogger(SwitchStartNodeOld.class);

	public SwitchStartNodeOld(String name, List<Data> dataIns, Data dataEval, List<Case> cases) {
		super(name, "");
		this.dataIns = dataIns;
		this.dataEval = dataEval;
		this.cases = cases;
	}

	/**
	 * Checks the dataValues and parses the switch condition. Depending on the input
	 * values a different switch case is executed.
	 */
	@Override
	public Boolean call() throws Exception {
		final Map<String, Object> switchInputValues = new HashMap<>();
		for (Data data : dataIns) {
			if (!dataValues.containsKey(data.getSource())) {
				throw new MissingInputDataException(
						SwitchStartNodeOld.class.getCanonicalName() + ": " + name + " needs " + data.getSource() + "!");
			} else {
				switchInputValues.put(name + "/" + data.getName(), dataValues.get(data.getSource()));
			}
		}
		if (!dataValues.containsKey(dataEval.getSource())) {
			throw new MissingInputDataException(
					SwitchStartNodeOld.class.getCanonicalName() + ": " + name + " needs " + dataEval.getSource() + "!");
		}

		logger.info("Executing " + name + " SwitchStartNodeOld");

		Object switchValue = parseSwitchCondition();
		// goes through all cases and executes a case if the switch value matches this
		// case
		for (int i = 0; i < cases.size(); i++) {
			if (caseMatches(cases.get(i).getValue(), switchValue)) {
				logger.info("Switch case " + cases.get(i).getValue() + " fulfilled with value " + switchValue);
				children.get(i).passResult(switchInputValues);
				children.get(i).call();
				return true;
			} else if (children.size() > cases.size()) {
				logger.info("Switch default case is executed.");
				children.get(children.size() - 1).passResult(switchInputValues);
				children.get(children.size() - 1).call();
				return true;
			}
		}
		throw new NoSwitchCaseFulfilledException(
				"No matching switch case found for value " + switchValue + " in node " + name);
	}

	/**
	 * Checks if the input value matches with the defined case. Integers and strings
	 * are supported.
	 * 
	 * @param object    The defined switch case value
	 * @param switchVal The input value
	 * @return true if switch case value matches the input value, otherwise false.
	 */
	private boolean caseMatches(Object object, Object switchVal) {
		switch (dataEval.getType()) {
		case "string":
			return object.equals((String) switchVal);
		case "number":
			return Integer.parseInt((String) object) == ((Integer) switchVal).intValue();
		default:
			logger.info("Unknown type for condition data type " + dataEval.getType());
		}
		return false;
	}

	/**
	 * Parses the input value for the switch condition.
	 * 
	 * @return A string or an integer with the value.
	 */
	private Object parseSwitchCondition() {
		switch (dataEval.getType()) {
		case "string":
			return (String) dataValues.get(dataEval.getSource());
		case "number":
			return (Integer) dataValues.get(dataEval.getSource());
		default:
			logger.info("Unknown type for condition data type " + dataEval.getType());
		}
		return null;

	}

	/**
	 * Sets the passed result as data values.
	 */
	@Override
	public void passResult(Map<String, Object> input) {
		synchronized (this) {
			if (dataValues == null)
				dataValues = new HashMap<>();
			for (Data data : dataIns) {
				if (input.containsKey(data.getSource())) {
					dataValues.put(data.getSource(), input.get(data.getSource()));
				}
			}
			if (input.containsKey(dataEval.getSource())) {
				dataValues.put(dataEval.getSource(), input.get(dataEval.getSource()));
			}
		}

	}

	@Override
	public Map<String, Object> getResult() {
		// TODO Auto-generated method stub
		return null;
	}

}