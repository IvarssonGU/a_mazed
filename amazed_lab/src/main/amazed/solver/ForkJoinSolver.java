package amazed.solver;

import amazed.maze.Maze;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <code>ForkJoinSolver</code> implements a solver for
 * <code>Maze</code> objects using a fork/join multi-thread
 * depth-first search.
 * <p>
 * Instances of <code>ForkJoinSolver</code> should be run by a
 * <code>ForkJoinPool</code> object.
 */


public class ForkJoinSolver extends SequentialSolver {

    private final ConcurrentLinkedQueue<ForkJoinSolver> forkedSolvers = new ConcurrentLinkedQueue<>();
    private static final Set<Integer> visited = new ConcurrentSkipListSet<>(); // Create a thread-safe set
    private final Stack<Integer> frontier = new Stack<>();
    private static final AtomicBoolean goalFound = new AtomicBoolean();
    private CopyOnWriteArrayList<Integer> result = new CopyOnWriteArrayList<>();

    /**
     * Creates a solver that searches in <code>maze</code> from the
     * start node to a goal.
     *
     * @param maze   the maze to be searched
     */
    public ForkJoinSolver(Maze maze, Integer next)
    {
        this(maze);
        start = next;
    }

    /**
     * Creates a solver that searches in <code>maze</code> from the
     * start node to a goal, forking after a given number of visited
     * nodes.
     *
     * @param maze        the maze to be searched
     * @param forkAfter   the number of steps (visited nodes) after
     *                    which a parallel task is forked; if
     *                    <code>forkAfter &lt;= 0</code> the solver never
     *                    forks new tasks
     */
    public ForkJoinSolver(Maze maze, int forkAfter)
    {
        this(maze);
        this.forkAfter = forkAfter;
    }

    public ForkJoinSolver(Maze maze) {
        super(maze);
    }

    /**
     * Searches for and returns the path, as a list of node
     * identifiers, that goes from the start node to a goal node in
     * the maze. If such a path cannot be found (because there are no
     * goals, or all goals are unreacheable), the method returns
     * <code>null</code>.
     <>
     * @return   the list of node identifiers from the start node to a
     *           goal node in the maze; <code>null</code> if such a path cannot
     *           be found.
     */
    @Override
    public List<Integer> compute() {return parallelSearch(start);}

    private List<Integer> parallelSearch(int start) {
        int player = maze.newPlayer(start);
        frontier.push(start);
        int current = start;

        do {
            if (goalFound.get()) return null;
            current = frontier.pop(); // get the new node to process

            if (maze.hasGoal(current)) {
                maze.move(player, current); //Move the player to the goal
                goalFound.set(true);
                result.addAll(pathFromTo(start, current));
                System.out.println(predecessor);
                System.out.println("goal child " + result);
                System.out.println("real start: " + maze.start());
                System.out.println("real end " + current);
                return result; // search finished: reconstruct and return path
            }

            if (visited.add(current)) { // if current node has not been visited yet
                maze.move(player, current); // move player to current node

                for (Integer nb: maze.neighbors(current)) {
                    // if nb has not been already visited,
                    // nb can be reached from current (i.e., current is nb's predecessor)
                    if (!visited.contains(nb)) {
                        frontier.push(nb); // add nb to the nodes to be processed
                        predecessor .put(nb, current);
                    }
                }
                switch (frontier.size()) {
                    case 1:
                        break;       // exit the switch case
                    case 2:
                        popAndAdd(); // fork new solver for the first neighbour
                    case 3:
                        popAndAdd(); // same logic as above
                        popAndAdd();
                }
            }
        } while (!frontier.isEmpty() && !goalFound.get()); //while loop end

        for (ForkJoinSolver task : forkedSolvers) {
            if (task.join() != null && !task.result.isEmpty()) {
                result.addAll(task.result);
            }
        }

        if (result != null && !result.isEmpty()) {
            result.addAll(pathFromTo(start, current));
            return result;
        } else {
            return null; //if no goal was ever found, return null
        }
    }

    /**
     * Pops an int from the frontier stack, creates a new ForkJoinSolver with all the necessary
     * parameters and adds this ForkJoinSolver to the list with tasks to be processed
     */
        private void popAndAdd() {
        if (!frontier.isEmpty()) {
            Integer next = frontier.pop();
            if (!visited.contains(next)) {
                ForkJoinSolver task = new ForkJoinSolver(maze, next);
                forkedSolvers.add(task);
                // alternativt testa att köra join på denna instans och vänta på dess barn.
                task.fork(); // Create chaos, uncomment when we have desired behavior
                //task.join(); //this makes the code work but not running in parallel
            }
        }
    }
}
