import com.smarthome.smart_home_backend.ds.engine.RuleEngine;
import com.smarthome.smart_home_backend.ds.model.*;
import com.smarthome.smart_home_backend.ds.structures.EventQueue;

/** Run with scripts/check-engine.sh; no Maven/Spring/SQL needed. */
public class EngineSmokeTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        EventQueue q = new EventQueue();
        q.enqueue(new SensorEvent(2, 1)); q.enqueue(new SensorEvent(3, 4));
        check(q.size() == 2, "queue size");
        q.dequeue(); check(q.size() == 1, "queue dequeue");
        RuleEngine engine = new RuleEngine();
        Rule r = new Rule(1, "Motion + temperature");
        r.addCondition(new Condition(1, ComparisonOperator.EQUAL, 1));
        r.addCondition(new Condition(2, ComparisonOperator.LESS_THAN, 18));
        r.addAction(new Action(10, "turn_on"));
        engine.addRule(r);
        check(engine.preview(1, 1).isEmpty(), "preview must require both conditions");
        engine.triggerEvent(2, 16, 10); engine.processAllEvents();
        check(engine.preview(1, 1).size() == 1, "preview should match when other reading known");
        check(!engine.getSensorStates().containsKey(1), "preview must not mutate state");
        engine.triggerEvent(1, 1, 11); engine.processAllEvents();
        check(engine.getExecutionLog().size() == 1, "one action planned");
        check(engine.getExecutionLog().get(0).getEventId() == 11, "persisted event ID retained");
        Rule second = new Rule(2, "Dependent");
        second.addCondition(new Condition(1, ComparisonOperator.EQUAL, 1));
        second.addAction(new Action(20, "turn_off"));
        engine.addRule(second);
        engine.addDependency(1, 2);
        boolean rejected = false;
        try { engine.addDependency(2, 1); } catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected && !engine.hasCycle(), "cycle rejection");
        check(ComparisonOperator.fromSymbol("=") == ComparisonOperator.EQUAL, "operator alias");
        System.out.println("PASS: queue, multi-condition matching, preview isolation, event ID, dependency cycle, aliases");
    }
}
