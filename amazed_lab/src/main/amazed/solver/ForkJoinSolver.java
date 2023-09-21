package amazed.solver;

import amazed.maze.Maze;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinPool;

/**
 * <code>ForkJoinSolver</code> implements a solver for
 * <code>Maze</code> objects using a fork/join multi-thread
 * depth-first search.
 * <p>
 * Instances of <code>ForkJoinSolver</code> should be run by a
 * <code>ForkJoinPool</code> object.
 */


public class ForkJoinSolver extends SequentialSolver {

    private final ForkJoinPool fjPool = new ForkJoinPool();
    private final List<ForkJoinSolver> forkedSolvers = new ArrayList<>(); // Keep track of forked tasks
    private Set<Integer> visited = new ConcurrentSkipListSet<>(); // Create a thread-safe set
    private Stack<Integer> frontier = new Stack<>();
    private Map<Integer, Integer> predecessor = new ConcurrentSkipListMap<>(); //Create a thread-safe Map


    /**
     * Creates a solver that searches in <code>maze</code> from the
     * start node to a goal.
     *
     * @param maze   the maze to be searched
     */
    public ForkJoinSolver(Maze maze, Set<Integer> visited, Map<Integer, Integer> predecessor, int next)
    {
        super(maze);
        this.visited = visited;
        this.predecessor = predecessor;
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

        while (!frontier.isEmpty()) {
            int current = frontier.pop(); // get the new node to process

            if (maze.hasGoal(current)) {
                maze.move(player, current); //Move the player to the goal
                return pathFromTo(start, current); // search finished: reconstruct and return path
            }

            if (!visited.contains(current)) { // if current node has not been visited yet
                maze.move(player, current); // move player to current node
                visited.add(current); // mark node as visited

                for (int nb: maze.neighbors(current)) {
                    frontier.push(nb); // add nb to the nodes to be processed

                    // if nb has not been already visited,
                    // nb can be reached from current (i.e., current is nb's predecessor)
                    if (!visited.contains(nb))
                        predecessor.put(nb, current);
                }
                switch (frontier.size()) {
                    case 0:
                        return null; //kan vara fel? låter vara kvar så länge
                    case 1:
                        break;       // exit the switch case
                    case 2:
                        popAndAdd(); //fork new solver for the first neighbour
                        popAndAdd(); //fork another for the second neighbour
                    case 3:
                        popAndAdd(); //same logic as above
                        popAndAdd();
                        popAndAdd();
                }

            }

            // to fork a new thread you just create a new instance of ForkJoinSolver,
            // with suitable parameters, and call fork() on the instance.

        } //while loop end

        for (ForkJoinSolver task : forkedSolvers) {
            fjPool.invoke(task);
        }

        return null; //if no goal was ever found, return null
    }

    /**
     * Pops a int from the frontier stack, creates a new ForkJoinSolver with all the necessary
     * parameters and adds this ForkJoinSolver to the list with tasks to be processed
     */
    private void popAndAdd() {
        if (!frontier.isEmpty()) {
            int next = frontier.pop();
            ForkJoinSolver task = new ForkJoinSolver(maze, visited, predecessor, next);
            forkedSolvers.add(task);
        }
    }
}
