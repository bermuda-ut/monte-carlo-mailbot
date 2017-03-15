package strategies;
import java.util.concurrent.ThreadLocalRandom;

import automail.*;

import java.util.*;

import static java.lang.Math.abs;

/**
 * Created by noxm on 13/03/17.
 */
public class AdvancedMailPool implements IMailPool {
    List<MailItem> mailPool;

    public static final int MAX_CAPACITY = (new StorageTube()).MAXIMUM_CAPACITY; // because its not static in StorageTube!

    public static final int MAX_BRANCHING_FACTOR = 5;
    public static final int MAX_DEPTH = 4;
    public static final int MIN_SCORE = 60;
    public static final int MAX_BRANCHES = 50;

    private static final double SUM_PRIORITY_FACTOR = 0.05;
    private static final double MAX_GAP_FACTOR = 1.25;
    private static final double MAX_DEST_FACTOR = 1.3;
    private static final double RATIO_FACTOR = 0.1;
    private static final double EFFIC_FACTOR = 0.3;

    public AdvancedMailPool() {
        mailPool = new ArrayList<>();
    }

    public static int getMailIntPriority(MailItem mailItem) {
        String priorityLevel = mailItem.getPriorityLevel();

        for(int i = 0; i < MailItem.PRIORITY_LEVELS.length; i++) {
            if (priorityLevel.equals(MailItem.PRIORITY_LEVELS[i])) {
                return i + 1;
            }
        }

        return 0;
    }

    public List<MailItem> getMails() {
        // get most effective combination
        List<List<MailItem>> combinations = new ArrayList<>();
        Stack<List<MailItem>> stack = new Stack<>();
        int branchCount = Math.min(MAX_BRANCHES, mailPool.size());

        Collections.shuffle(mailPool, new Random());
        for(int m = 0; m < branchCount; m++) {
            List<MailItem> currList = new ArrayList<>();
            currList.add(mailPool.get(m));
            stack.add(currList);
        }
        Collections.shuffle(stack, new Random());

        while(stack.size() > 0) {
//        for(int b = 0; b < branchCount; b++) {
//            System.out.println(combinations.size() + " " + stack.size());
//            System.out.println(stack);
            List<MailItem> currCombination = stack.pop();
            double currScore = combinationScore(currCombination);
            boolean modified = false;

            if(currCombination.size() >= MAX_DEPTH) {
                combinations.add(currCombination);
                continue;
            }

            Collections.shuffle(mailPool);
            for(int i = 0; i < branchCount; i++) {
                MailItem toAdd = mailPool.get(i);

                boolean effective = false;
                if((toAdd.getDestFloor() > Building.MAILROOM_LOCATION && currCombination.get(0).getDestFloor() > Building.MAILROOM_LOCATION) ||
                   (toAdd.getDestFloor() < Building.MAILROOM_LOCATION && currCombination.get(0).getDestFloor() < Building.MAILROOM_LOCATION))
                    effective = true;

                if(!effective)
                    continue;

                // not found in current combination
                if(currCombination.indexOf(toAdd) < 0) {
                    List<MailItem> newCombination = new ArrayList<>(currCombination);
                    newCombination.add(toAdd);

                    double newScore = combinationScore(newCombination);
                    if(newScore >= currScore && totalSize(newCombination) <= MAX_CAPACITY) {
                        stack.add(newCombination);
                        modified = true;
                        break;
                    }
                }
            }

            if(!modified) {
                combinations.add(currCombination);
            }
        }

        /*
        // generate tree and get the final leaves
        int depth = (MAX_CAPACITY < MAX_DEPTH) ? MAX_CAPACITY : MAX_DEPTH;

        for(int i = 0; i < depth; i++) {
            List<List<MailItem>> branch = new ArrayList<>();

            for(List<MailItem> combination: combinations) {
                int totalSize = totalSize(combination);
                boolean branchedOut = false;

                if(totalSize < MAX_CAPACITY) {
                    int branchingFactor = (mailPool.size() < MAX_BRANCHING_FACTOR)? mailPool.size(): MAX_BRANCHING_FACTOR;
                    int trial = 0;

                    for(int j = 0; j < branchingFactor; j++) {
                        int randomNum = ThreadLocalRandom.current().nextInt(0, mailPool.size());
                        MailItem mailItem = mailPool.get(randomNum);

                        // if not in the pool
                        if(combinationScore(combination) >= MIN_SCORE) {
                            continue;
                        }

                        if(combination.indexOf(mailItem) < 0) {
                            List<MailItem> newCombination = new ArrayList<>();
                            newCombination.addAll(combination);
                            newCombination.add(mailItem);
                            // add the combination
                            if(totalSize(newCombination) <= MAX_CAPACITY) {
                                branchedOut = true;
                                branch.add(newCombination);
                            }
                        }
                    }
                }

                if(!branchedOut)
                    branch.add(combination);
            }

            combinations = branch;
        }
        */

        // get most efficient combination
        double maxScore = 0;
        List<MailItem> curr = combinations.get(0);

        for(List<MailItem> combination: combinations) {
            combination.sort((a ,b) -> {
                if(a.getDestFloor() < b.getDestFloor())
                    return -1;
                else if(a.getDestFloor() > b.getDestFloor())
                    return 1;
                return 0;
            });

            double score = combinationScore(combination);
            if(score > maxScore) {
                curr = combination;
                maxScore = score;
            }
        }

        mailPool.removeAll(curr);
        return curr;
    }

    private int minFloor(List<MailItem> mailList) {
        int min = mailList.get(0).getDestFloor();
        for(MailItem mailItem : mailList) {
            if(min > mailItem.getDestFloor())
                min = mailItem.getDestFloor();
        }

        return min;
    }

    private int maxFloor(List<MailItem> mailList) {
        int max = mailList.get(0).getDestFloor();
        for(MailItem mailItem : mailList) {
            if(max < mailItem.getDestFloor())
                max = mailItem.getDestFloor();
        }
        return max;
    }

    private double simulateDeliveryScore(List<MailItem> deliveryItems) {
        // from how it is cacluated in real simulation
        final double penalty = 1.1;
        int currTime = Clock.Time();
        int currFloor = Building.MAILROOM_LOCATION;
        double score = 0;

        // Determine the priority_weight
        for(MailItem deliveryItem: deliveryItems) {
            double priority_weight = 0;
            switch(deliveryItem.getPriorityLevel()){
                case "LOW":
                    priority_weight = 1;
                    break;
                case "MEDIUM":
                    priority_weight = 1.5;
                    break;
                case "HIGH":
                    priority_weight = 2;
                    break;
            }

            score += Math.pow(currTime - deliveryItem.getArrivalTime(),penalty)*priority_weight;
            currTime += Math.abs(currFloor - deliveryItem.getDestFloor());
            currFloor = deliveryItem.getDestFloor();
        }
        score += Math.abs(currFloor - Building.MAILROOM_LOCATION);

        return score;
    }

    private double getEfficiency(List<MailItem> mailItemList) {
        int steps = getSteps(mailItemList);
        double score = simulateDeliveryScore(mailItemList);

        return 1.0 * score/steps;
    }

    private int getSteps(List<MailItem> mailList) {
        int steps = mailList.size();

        if(maxFloor(mailList) > Building.MAILROOM_LOCATION)
            steps += maxFloor(mailList);
        if(minFloor(mailList) < Building.MAILROOM_LOCATION)
            steps += maxFloor(mailList);

        return steps;
    }

    private double combinationScore(List<MailItem> mailList) {
        int sumPriority = 0;
        int maxGap = maxGap(mailList) + 1;

        int maxFloor = maxFloor(mailList);
        int minFloor = minFloor(mailList);
        int minFactor = Math.min(minFloor, Building.MAILROOM_LOCATION);
        int maxFactor = Math.max(maxFloor, Building.MAILROOM_LOCATION);
        int maxDest = maxFactor - minFactor;

        double sumScore = SUM_PRIORITY_FACTOR * sumPriority;
        double gapScore = MAX_GAP_FACTOR / maxGap;
        double destScore = MAX_DEST_FACTOR / maxDest;
        double ratiScore = RATIO_FACTOR * totalSize(mailList) / MAX_CAPACITY;
        double efficiency = EFFIC_FACTOR * getEfficiency(mailList);

        double finalScore = sumScore + gapScore + destScore + ratiScore;
        return finalScore * efficiency;
    }

    private int maxGap(List<MailItem> mailList) {
        int min = 1;
        int max = 1;

        for(MailItem mailItem : mailList) {
            int floor = mailItem.getDestFloor();
            if(floor > max)
                max = floor;
            if(floor < min)
                min = floor;
        }

        return max - min;
    }

    private int totalSize(List<MailItem> mailList) {
        int total = 0;
        for(MailItem mailItem : mailList) {
            total += mailItem.getSize();
        }
        return total;
    }

    @Override
    public void addToPool(MailItem mailItem) {
        mailPool.add(mailItem);
    }

    public boolean isEmptyPool() {
        if(mailPool.size() > 0)
            return false;
        return true;
    }

    public int size() {
        return mailPool.size();
    }
}
