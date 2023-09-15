package amazed.solver;

import amazed.maze.Maze;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
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


public class ForkJoinSolver
    extends SequentialSolver
{
    /**
     * Creates a solver that searches in <code>maze</code> from the
     * start node to a goal.
     *
     * @param maze   the maze to be searched
     */
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
     *
     * @return   the list of node identifiers from the start node to a
     *           goal node in the maze; <code>null</code> if such a path cannot
     *           be found.
     */
    @Override
    public List<Integer> compute() {return parallelSearch(start);}

    private List<Integer> parallelSearch(int current) { // int current

        int player = maze.newPlayer(current);
        frontier.push(current);

        //while (!frontier.isEmpty()) {s
            // get the new node to process


            // if current node has a goal
        if (maze.hasGoal(current)) {
            // move player to goal
            maze.move(player, current);
            // search finished: reconstruct and return path
            return pathFromTo(start, current);
        }

        if (!visited.contains(current)) {
            // move player to current node
            maze.move(player, current);
            frontier.pop();
            // mark node as visited
            visited.add(current);
            // for every node nb adjacent to current
            for (int nb : maze.neighbors(current)) {
                if (!visited.contains(nb)) {
                    frontier.push(nb);
                    predecessor.put(nb, current);
                }
            }
        }

        // to fork a new thread you just create a new instance of ForkJoinSolver,
        // with suitable parameters, and call fork() on the instance.
        if (frontier.size() > 1) {
            int next = frontier.pop();
            ForkJoinSolver newSolverBorkShmork = new ForkJoinSolver(maze);
            //newSolverBorkShmork.parallelSearch(next);
            newSolverBorkShmork.parallelSearch(next);
        }


        //}

        return null;
    }
}
