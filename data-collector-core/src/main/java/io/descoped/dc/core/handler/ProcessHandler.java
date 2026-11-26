package io.descoped.dc.core.handler;

import io.descoped.dc.api.Processor;
import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.handler.Handler;
import io.descoped.dc.api.http.Response;
import io.descoped.dc.api.node.Process;
import io.descoped.dc.api.services.ObjectCreator;

/**
 * Executes a {@code Process} node: creates the node's processor with the context's services, runs it with the
 * response, and returns its output. It throws {@code IllegalStateException} when the output lacks any of the node's
 * required output variables.
 */
@Handler(forClass = Process.class)
public class ProcessHandler extends AbstractNodeHandler<Process> {

    /**
     * Creates the handler of a node.
     *
     * @param process the node to execute
     */
    public ProcessHandler(Process process) {
        super(process);
    }

    @Override
    public ExecutionContext execute(ExecutionContext input) {
        super.execute(input);
        Processor processInstance = ObjectCreator.newInstance(node.processorClass(), input.services());

        ExecutionContext processInput = ExecutionContext.of(input);
        processInput.state(Response.class, input.state(Response.class));

        ExecutionContext processorOutput = processInstance.process(processInput);

        if (!node.requiredOutputs().stream().allMatch(key -> processorOutput.variables().containsKey(key))) {
            throw new IllegalStateException("A variable is missing!");
        }

        return processorOutput;
    }

}
