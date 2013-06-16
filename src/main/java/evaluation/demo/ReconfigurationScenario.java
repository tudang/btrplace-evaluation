package evaluation.demo;

import btrplace.model.Mapping;
import btrplace.model.Model;
import btrplace.model.Node;
import btrplace.model.VM;
import btrplace.model.constraint.*;
import btrplace.model.view.ShareableResource;
import btrplace.plan.ReconfigurationPlan;
import btrplace.solver.SolverException;
import btrplace.solver.choco.ChocoReconfigurationAlgorithm;
import btrplace.solver.choco.DefaultChocoReconfigurationAlgorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * User: Tu Huynh Dang
 * Date: 6/16/13
 * Time: 2:52 PM
 */
public abstract class ReconfigurationScenario {
    final static int NUM_CLUSTERS = 16;
    final static int NODES_PER_CLUSTER = 16;
    Model model;
    boolean restriction;
    EvaluateConstraint eval_constraint;
    ChocoReconfigurationAlgorithm cra = new DefaultChocoReconfigurationAlgorithm();
    ReconfigurationPlan plan = null;
    Collection<Collection<Node>> nss;

    public abstract ReconfigurationPlan reconfigure(Collection<Application> apps, int p) throws SolverException;

    public void initMap() {
        ShareableResource cpu = new ShareableResource("cpu", 64, 4);
        model.attach(cpu);
//        ShareableResource ram = new ShareableResource("ram", 128, 1);
//        model.attach(ram);
        for (int j = 0; j < NUM_CLUSTERS; j++) {
            ArrayList<Node> ns = new ArrayList<Node>();
            for (int i = 0; i < NODES_PER_CLUSTER; i++) {
                Node node = model.newNode();
                ns.add(node);
                model.getMapping().addOnlineNode(node);
            }
            if (j < 4) nss.add(ns);
        }
    }

    public Collection<Application> runWithSpread() {
        Collection<Application> totalApps = new ArrayList<Application>();
        try {
            do {
                Collection<Application> apps = new ArrayList<Application>();
                Collection<SatConstraint> cstrs = new ArrayList<SatConstraint>();
                for (int i = 0; i < 20; i++) {
                    Application app = new Application(model);
                    apps.add(app);
                    cstrs.addAll(app.spread(restriction));
                }

                plan = cra.solve(model, cstrs);

                if (plan != null) model = plan.getResult();
                totalApps.addAll(apps);
            } while ((plan != null) && currentLoad() < 70);
        } catch (SolverException e) {
            System.err.println("Run " + e.getMessage());
        }
        return totalApps;
    }

    public Collection<? extends Application> runWithGather() {
        Collection<Application> totalApps = new ArrayList<Application>();
        try {
            do {
                Collection<Application> apps = new ArrayList<Application>();
                Collection<SatConstraint> cstrs = new ArrayList<SatConstraint>();
                for (int i = 0; i < 20; i++) {
                    Application app = new Application(model);
                    apps.add(app);
                    cstrs.addAll(app.gather(restriction));
                }
                plan = cra.solve(model, cstrs);
                if (plan != null) {
                    model = plan.getResult();
                    totalApps.addAll(apps);
                }
            } while ((plan != null) && currentLoad() < 70);
        } catch (SolverException e) {
            System.err.println("Run " + e.getMessage());
        }
        return totalApps;
    }

    public Collection<? extends Application> runWithAmong() {
        Collection<Application> totalApps = new ArrayList<Application>();
        try {
            do {
                Collection<Application> apps = new ArrayList<Application>();
                Collection<SatConstraint> cstrs = new ArrayList<SatConstraint>();
                for (int i = 0; i < 20; i++) {
                    Application app = new Application(model);
                    apps.add(app);
                    Among among = new Among(app.getDatabaseVM(), nss, restriction);
                    app.setCheckConstraints(Collections.<SatConstraint>singleton(among));
                    Running run = new Running(app.getAllVM());
                    cstrs.add(among);
                    cstrs.add(run);
                }
                plan = cra.solve(model, cstrs);
                if (plan != null) model = plan.getResult();

                totalApps.addAll(apps);
            } while ((plan != null) && currentLoad() < 70);

        } catch (SolverException e) {
        }

        return totalApps;
    }

    public Collection<? extends Application> runWithLonely() {
        Collection<Application> totalApps = new ArrayList<Application>();
        try {
            do {
                Collection<Application> apps = new ArrayList<Application>();
                Collection<SatConstraint> cstrs = new ArrayList<SatConstraint>();
                for (int i = 0; i < 20; i++) {
                    Application app = new Application(model);
                    apps.add(app);
                    Running run = new Running(app.getAllVM());
                    cstrs.add(run);
                }
                cstrs.addAll(new Application(model).lonely(restriction));
                plan = cra.solve(model, cstrs);
                if (plan != null) model = plan.getResult();

                totalApps.addAll(apps);
            } while ((plan != null) && currentLoad() < 70);

        } catch (SolverException e) {
        }
        return totalApps;
    }

    public Collection<? extends Application> runWithSReC() {
        Collection<Application> totalApps = new ArrayList<Application>();
        try {
            do {
                Collection<Application> apps = new ArrayList<Application>();
                Collection<SatConstraint> cstrs = new ArrayList<SatConstraint>();
                for (int i = 0; i < 20; i++) {
                    Application app = new Application(model);
                    apps.add(app);
                    Running run = new Running(app.getAllVM());
                    cstrs.add(run);
                }
                SingleResourceCapacity SReC = new SingleResourceCapacity(model.getNodes(), "cpu", 60);
                Application application = new Application(model);
                application.setCheckConstraints(SReC);
                apps.add(application);
                cstrs.add(SReC);
                plan = cra.solve(model, cstrs);
                if (plan != null) model = plan.getResult();

                totalApps.addAll(apps);
            } while ((plan != null) && currentLoad() < 70);

        } catch (SolverException e) {
        }
        return totalApps;
    }

    public int currentLoad() {
        Mapping mapping = model.getMapping();
        Set<Node> onlineNodes = mapping.getOnlineNodes();
        Set<VM> runningVMs = mapping.getRunningVMs();
        ShareableResource sr = (ShareableResource) model.getView("ShareableResource.cpu");
        int capacity = sr.sumCapacities(onlineNodes, true);
        int used = sr.sumConsumptions(runningVMs, true);
        return (100 * used) / capacity;
    }

    enum EvaluateConstraint {
        spread, among, gather, lonely, SReC
    }

}
