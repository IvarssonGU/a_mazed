package amazed.solver;

import amazed.maze.Maze;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <code>ForkJoinSolver</code> implements a solver for
 * <code>Maze</code> objects using a fork/join multi-thread
 * depth-first search.
 * <p>
 * Instances of <code>ForkJoinSolver</code> should be run by a
 * <code>ForkJoinPool</code> object.
 */


public class ForkJoinSolver extends SequentialSolver {

    private static ForkJoinPool fjPool = new ForkJoinPool();;
    //private Set<Integer> visited = new ConcurrentSkipListSet<>(); // Create a thread-safe set
    private static ConcurrentLinkedDeque<Integer> frontier = new ConcurrentLinkedDeque<>(); // Create a thread-safe stack
    //private final Lock visitedLock = new ReentrantLock();
    private static Set<Integer> visited = new ConcurrentSkipListSet<>();

    private Map<Integer, Integer> predecessor;

    private int current;

    /**
     * Creates a solver that searches in <code>maze</code> from the
     * start node to a goal.
     *
     * @param maze   the maze to be searched
     */

    // Skapa en ForkJoinPool-instans, fixa workers osv kanske något arbete mm mm dsvdv.
    public ForkJoinSolver(Maze maze, int forkAfter, Map<Integer, Integer> predecessor, Set<Integer> visited, int current)
    {
        super(maze);
        this.forkAfter = forkAfter;
        this.predecessor = predecessor;
        this.current = current;
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
    public ForkJoinSolver(Maze maze, int forkAfter  )
    {
        super(maze);
        this.forkAfter = forkAfter;
        this.predecessor = new HashMap<>();
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

    private List<Integer> parallelSearch(int current) {
        int player = maze.newPlayer(current);
        frontier.push(current);
        List<ForkJoinSolver> forkedSolvers = new ArrayList<>(); // Keep track of forked tasks
        int nextPos = frontier.pop(); // get the new node to process

        if (maze.hasGoal(nextPos)) {
            maze.move(player, nextPos); //Move the player to the goal
            return pathFromTo(start, nextPos); // search finished: reconstruct and return path
        }

        if (!visited.contains(nextPos)) { // if current node has not been visited yet
            visited.add(nextPos); // mark node as visited
            maze.move(player, nextPos); // move player to current node

            for (int nb : maze.neighbors(nextPos)) {
                frontier.push(nb); // add nb to the nodes to be processed

                // if nb has not been already visited,
                // nb can be reached from current (i.e., current is nb's predecessor)
                if (!visited.contains(nb)) {
                    predecessor.put(nextPos, nb);
                }
            }
        }

        if (frontier.size() == 1) {
            Integer next = frontier.pop();
            maze.move(player, next);
            return parallelSearch(next);
        } else if (frontier.size() == 2) {
            Integer next = frontier.pop();
            maze.move(player, next);
            forkedSolvers.add(new ForkJoinSolver(maze, forkAfter, predecessor, visited, frontier.pop()));
            return parallelSearch(next);
            //(frontier.size() > 1) {
            //for (int nb : frontier) {
            //    forkedSolvers.add(new ForkJoinSolver(maze, forkAfter, predecessor, visited, nb));
            //}
        } else if (frontier.size() == 3) {
            Integer next = frontier.pop();
            maze.move(player, next);
            forkedSolvers.add(new ForkJoinSolver(maze, forkAfter, predecessor, visited, frontier.pop()));
            forkedSolvers.add(new ForkJoinSolver(maze, forkAfter, predecessor, visited, frontier.pop()));
            return parallelSearch(next);
        } else {
            return null;
        }

        // Forked tasks are created, now fork and join them outside the loop to ensure parallelism
        for (ForkJoinSolver solver : forkedSolvers) {
            fjPool.submit(solver); // Submit the solver to the pool for parallel execution
        }

        // Join and wait for all forked tasks to complete
        for (ForkJoinSolver solver : forkedSolvers) {
            solver.join();
        }
    }
}
