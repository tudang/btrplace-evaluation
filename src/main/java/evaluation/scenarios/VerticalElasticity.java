package evaluation.scenarios;

import btrplace.model.VM;
import btrplace.model.constraint.Preserve;
import btrplace.model.constraint.SatConstraint;
import btrplace.plan.ReconfigurationPlan;
import btrplace.solver.SolverException;
import evaluation.demo.Application;

import java.util.*;

/**
 * User: TU HUYNH DANG
 * Date: 6/18/13
 * Time: 11:34 AM
 */
public class VerticalElasticity extends ReconfigurationScenario {


    public VerticalElasticity(String mfile, String appFile, String out) {
        super(mfile, appFile, out);
        rp_type = "ve";
    }

    public static void main(String[] args) {
        ReconfigurationScenario instance = new VerticalElasticity(args[0], args[1], args[2]);
        instance.findContinuous();
        instance.run();
    }


    public void run() {
        readData();
        int p = 10;
        if (findContinuous) {
            reconfigure(p, true);
        } else reconfigure(p, false);
        System.out.print(this);
    }

    @Override
    public boolean reconfigure(int p, boolean c) {
        int DCconstraint[] = new int[2];
        HashSet<Integer>[] violatedConstraints = new HashSet[3];
        HashSet<Integer> affectedApps = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            violatedConstraints[i] = new HashSet<>();
        }
        boolean satisfied = true;
        Collection<SatConstraint> constraints = new ArrayList<>();
        ReconfigurationPlan plan;
        Collections.shuffle(new ArrayList<>(applications));
        int size = applications.size() * p / 100;
        Iterator<Application> iterator = applications.iterator();
        Collection<VM> tier1 = new ArrayList<>();
        Collection<VM> tier2 = new ArrayList<>();
        Collection<VM> tier3 = new ArrayList<>();
        while (iterator.hasNext() && size > 0) {
            Application randomApp = iterator.next();
            tier1.addAll(randomApp.getTier1());
            tier2.addAll(randomApp.getTier2());
            tier3.addAll(randomApp.getTier3());
            size--;
        }
        constraints.add(new Preserve(tier1, "ecu", 2));
        constraints.add(new Preserve(tier1, "ram", 4));
        constraints.add(new Preserve(tier2, "ecu", 14));
        constraints.add(new Preserve(tier2, "ram", 7));
        constraints.add(new Preserve(tier3, "ecu", 4));
        constraints.add(new Preserve(tier3, "ram", 17));

        if (c) {
            for (SatConstraint s : validateConstraint) {
                s.setContinuous(true);
            }
        }
        constraints.addAll(validateConstraint);
        try {
            plan = cra.solve(model, constraints);
            if (plan == null) {
                sb.append(String.format("%d\tNo solution\n", modelId));
                return false;
            } else {
                checkSatisfaction(plan, violatedConstraints, DCconstraint, affectedApps);
            }
        } catch (SolverException e) {
            sb.append(String.format("%d\t%s\n", modelId, e.getMessage()));
            return false;
        }
        result(plan, violatedConstraints, DCconstraint, affectedApps);
        return satisfied;
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
