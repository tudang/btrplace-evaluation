package evaluation.scenarios;

import btrplace.model.Node;
import btrplace.model.VM;
import btrplace.model.constraint.Offline;
import btrplace.model.constraint.Running;
import btrplace.model.constraint.SatConstraint;
import btrplace.plan.ReconfigurationPlan;
import btrplace.solver.SolverException;
import btrplace.solver.choco.SolvingStatistics;
import evaluation.generator.ConverterTools;

import java.util.*;

/**
 * User: TU HUYNH DANG
 * Date: 6/19/13
 * Time: 9:42 AM
 */
public class ServerFailures extends ReconfigurationScenario {

    Collection<Node> failedNodes;
    Collection<VM> restartVMs;

    public ServerFailures(int id) {
        modelId = id;
        sb = new StringBuilder();
        failedNodes = new ArrayList<>();
        restartVMs = new ArrayList<>();
        cra.setTimeLimit(TIME_OUT);
        cra.doRepair(true);
    }

    public static void main(String[] args) {
        ReconfigurationScenario instance = new ServerFailures(1);
        instance.run();
    }

    @Override
    public void run() {
        readData(modelId);
        int p = 8;
        List<Node> nodes = new ArrayList<>(model.getMapping().getAllNodes());
        int size = p * nodes.size() / 100;
        Collections.shuffle(nodes);
        for (int i = 0; i < size; i++) {
            Node e = nodes.get(i);
            Set<VM> runningVMs = model.getMapping().getRunningVMs(e);
            restartVMs.addAll(runningVMs);
            model.getMapping().clearNode(e);
            if (!model.getMapping().addOfflineNode(e)) {
                System.err.println("Remove node failed");
                System.exit(0);
            } else failedNodes.add(e);
        }
        for (Node n : failedNodes) {
            if (model.getMapping().getOnlineNodes().contains(n)) {
                System.err.println("Still run");
                System.exit(0);
            }
        }
        for (VM vm : restartVMs) {
            model.getMapping().addReadyVM(vm);
        }
        reconfigure(size, false);
        reconfigure(size, true);

        System.out.print(sb.toString());
    }

    @Override
    boolean reconfigure(int p, boolean c) {
        int DCconstraint[] = new int[2];
        ArrayList<ArrayList<Integer>> violatedApp = new ArrayList<>();
        HashSet<Integer> affectedApps = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            violatedApp.add(new ArrayList<Integer>());
        }
        boolean satisfied = true;
        Collection<SatConstraint> constraints = new ArrayList<>();
        ReconfigurationPlan plan;
        constraints.add(new Running(restartVMs));
        constraints.add(new Offline(failedNodes));
        if (c) {
            for (SatConstraint s : validateConstraint) {
                s.setContinuous(true);
            }
        }
        constraints.addAll(validateConstraint);
        try {
            plan = cra.solve(model, constraints);
            if (plan == null) {
                sb.append(String.format("Model %d\t %b \t No solution\n", modelId, c));
                return false;
            } else {
                for (Node n : failedNodes) {
                    if (plan.getResult().getMapping().getOnlineNodes().contains(n)) {
                        System.err.println("Failed servers run again");
                    }
                }
                checkSatisfaction(plan, violatedApp, DCconstraint, affectedApps);
            }
        } catch (SolverException e) {
            sb.append(String.format("Model %d.\t%b\t%s\n", modelId, c, e.getMessage()));
            return false;
        }

        String path = System.getProperty("user.home") + System.getProperty("file.separator") + "plan"
                + System.getProperty("file.separator") + "sf" + System.getProperty("file.separator");

        ConverterTools.planToFile(plan, String.format("%s%d%b", path, modelId, c));

        sb.append(String.format("%-2d\t%b\t%-3d\t%-2d\t%d\t%d\t%d\t%d\t%d\t", modelId, c, p,
                violatedApp.get(0).size(), violatedApp.get(1).size(), violatedApp.get(2).size(),
                DCconstraint[0], DCconstraint[1], affectedApps.size()));
        float[] load = currentLoad(model);
        sb.append(String.format("%f\t%f\t", load[0], load[1]));
        load = currentLoad(plan.getResult());
        sb.append(String.format("%f\t%f\t", load[0], load[1]));
        SolvingStatistics statistics = cra.getSolvingStatistics();
        sb.append(String.format("%d\t%d\t%d\n", statistics.getSolvingDuration(), plan.getDuration(), plan.getSize()));
        return satisfied;
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
