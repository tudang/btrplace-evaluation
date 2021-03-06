package evaluation.scenarios;

import btrplace.model.VM;
import btrplace.model.constraint.Preserve;
import btrplace.model.constraint.Running;
import btrplace.model.constraint.SatConstraint;
import btrplace.plan.ReconfigurationPlan;
import btrplace.solver.SolverException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 * User: TU HUYNH DANG
 * Date: 6/19/13
 * Time: 10:05 AM
 */
public class BootStorm extends ReconfigurationScenario {

    public BootStorm(String mfile, String appFile, String out) {
        super(mfile, appFile, out);
        rp_type = "bs";
    }

    public static void main(String[] args) {
        ReconfigurationScenario instance = new BootStorm(args[0], args[1], args[2]);
        instance.run();
    }

    @Override
    public void run() {
        readData();
        int p = 400;
        if (findContinuous)
            reconfigure(p, true);
        else
            reconfigure(p, false);
        System.out.print(this);
    }

    @Override
    public boolean reconfigure(int p, boolean c) {
        int DCconstraint[] = new int[2];
        HashSet<Integer> violatedConstraints[] = new HashSet[3];
        HashSet<Integer> affectedApps = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            violatedConstraints[i] = new HashSet<>();
        }
        boolean satisfied = true;
        int currentVmId = model.getMapping().getAllVMs().size();
        Collection<SatConstraint> cstrs = new ArrayList<>();
        ReconfigurationPlan plan;
        Collection<VM> bootVMS1 = new ArrayList<>();
        Collection<VM> bootVMS2 = new ArrayList<>();
        Collection<VM> bootVMS3 = new ArrayList<>();
        for (int i = 0; i < p; i++) {
            VM vm = model.newVM(currentVmId++);
            model.getMapping().addReadyVM(vm);
            if (i % 3 == 0) bootVMS1.add(vm);
            else if (i % 3 == 1) bootVMS2.add(vm);
            else bootVMS3.add(vm);
        }
        cstrs.add(new Running(bootVMS1));
        cstrs.add(new Running(bootVMS2));
        cstrs.add(new Running(bootVMS3));
        cstrs.add(new Preserve(bootVMS1, "ecu", 2));
        cstrs.add(new Preserve(bootVMS1, "ram", 4));
        cstrs.add(new Preserve(bootVMS2, "ecu", 14));
        cstrs.add(new Preserve(bootVMS2, "ram", 7));
        cstrs.add(new Preserve(bootVMS3, "ecu", 4));
        cstrs.add(new Preserve(bootVMS3, "ram", 17));

        if (c) {
            for (SatConstraint s : validateConstraint) {
                s.setContinuous(true);
            }
        }

        cstrs.addAll(validateConstraint);
        try {
            plan = cra.solve(model, cstrs);
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
