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

/**
 * <code>ForkJoinSolver</code> implements a solver for
 * <code>Maze</code> objects using a fork/join multi-thread
 * depth-first search.
 * <p>
 * Instances of <code>ForkJoinSolver</code> should be run by a
 * <code>ForkJoinPool</code> object.
 */


public class ForkJoinSolver extends SequentialSolver {

    ForkJoinPool fjPool = new ForkJoinPool(); //(kanske här)

    private Set<Integer> visited = new ConcurrentSkipListSet<>(); // Create a thread-safe set
    ConcurrentLinkedDeque<Integer> frontier = new ConcurrentLinkedDeque<>(); // Create a thread-safe stack




    /**
     * Creates a solver that searches in <code>maze</code> from the
     * start node to a goal.
     *
     * @param maze   the maze to be searched
     */

    // Skapa en ForkJoinPool-instans, fixa workers osv kanske något arbete mm mm dsvdv.
    public ForkJoinSolver(Maze maze)
    {
        super(maze);
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
            }

            // to fork a new thread you just create a new instance of ForkJoinSolver,
            // with suitable parameters, and call fork() on the instance.
            if (frontier.size() > 1) {
                int next = frontier.pop();
                // Create a new ForkJoinSolver instance with the current maze, player position, and forkAfter value
                ForkJoinSolver newSolverBorkShmork = new ForkJoinSolver(maze, forkAfter);
                maze.move(player, next);
                newSolverBorkShmork.fork();
                newSolverBorkShmork.join();
            }
        }

        return null;
    }
}
