package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.PowerUps;
import za.co.entelect.challenge.enums.Terrain;

import java.util.*;

import static java.lang.Math.max;

import java.security.SecureRandom;

public class Bot {

    private List<Command> directionList = new ArrayList<>();

    private final Random random;

    private final static Command ACCELERATE = new AccelerateCommand();
    private final static Command USE_BOOST = new BoostCommand();
    private final static Command TURN_LEFT = new ChangeLaneCommand(-1);
    private final static Command TURN_RIGHT = new ChangeLaneCommand(1);
    private final static Command DECELERATE = new DecelerateCommand();
    private final static Command NOTHING = new DoNothingCommand();
    private final static Command USE_EMP = new EmpCommand();
    private final static Command FIX = new FixCommand();
    private final static Command USE_LIZARD = new LizardCommand();
    private final static Command USE_OIL = new OilCommand();
    // private final static Command USE_TWEET = new TweetCommand(lane, block);

    public Bot() {
        this.random = new SecureRandom();
        directionList.add(TURN_LEFT);
        directionList.add(TURN_RIGHT);
    }

    public Command run(GameState gameState) {
        Car myCar = gameState.player;
        Car opponent = gameState.opponent;

        //Basic fix logic
        List<Object> blocks = getBlocksInFront(myCar.position.lane, myCar.position.block, gameState);
        // List<Object> blocksLeft = getBlocksInFrontLeft(myCar.position.lane, myCar.position.block, gameState);
        // List<Object> blocksRight = getBlocksInFrontRight(myCar.position.lane, myCar.position.block, gameState);
        List<Object> nextBlocks = blocks.subList(0,myCar.speed);
        // List<Object> nextBlocksLeft = blocksLeft.subList(0,maxSpeed+1);
        // List<Object> nextBlocksRight = blocksRight.subList(0,maxSpeed+1);

        //Fix first if too damaged to move
        if (myCar.damage > 1) {
            return FIX;
        }
        
        //Basic avoidance logic
        /* if (nextBlocks.contains(Terrain.MUD) || nextBlocks.contains(Terrain.OIL_SPILL) || nextBlocks.contains(Terrain.WALL) || nextBlocks.contains(Terrain.TWEET)) {
            if (myCar.position.lane != 1 && myCar.position.lane != 4) {
                if (nextBlocksLeft.contains(Terrain.MUD) || nextBlocksLeft.contains(Terrain.OIL_SPILL) || nextBlocksLeft.contains(Terrain.WALL) || nextBlocksLeft.contains(Terrain.TWEET)) {
                    if (nextBlocksRight.contains(Terrain.MUD) || nextBlocksRight.contains(Terrain.OIL_SPILL) || nextBlocksRight.contains(Terrain.WALL) || nextBlocksRight.contains(Terrain.TWEET)) {
                        if (hasPowerUp(PowerUps.LIZARD, myCar.powerups)) {
                            return USE_LIZARD;
                        } else {
                            if (nextBlocks.contains(Terrain.WALL) || nextBlocks.contains(Terrain.TWEET)) {
                                if (nextBlocksLeft.contains(Terrain.WALL) || nextBlocksLeft.contains(Terrain.TWEET)) {
                                    if (nextBlocksRight.contains(Terrain.WALL) || nextBlocksRight.contains(Terrain.TWEET)) {
                                        return NOTHING;
                                    } else {
                                        return TURN_RIGHT;
                                    }
                                } else {
                                    return TURN_LEFT;
                                }
                            } else {
                                return NOTHING;
                            }
                        }
                    } else {
                        return TURN_RIGHT;
                    }
                } else {
                    return TURN_LEFT;
                }
            } else {
                if (myCar.position.lane == 1) {
                    if (nextBlocksRight.contains(Terrain.MUD) || nextBlocksRight.contains(Terrain.OIL_SPILL) || nextBlocksRight.contains(Terrain.WALL) || nextBlocksRight.contains(Terrain.TWEET)) {
                        if (hasPowerUp(PowerUps.LIZARD, myCar.powerups)) {
                            return USE_LIZARD;
                        } else {
                            if (nextBlocks.contains(Terrain.WALL) || nextBlocks.contains(Terrain.TWEET)) {
                                if (nextBlocksRight.contains(Terrain.WALL) || nextBlocksRight.contains(Terrain.TWEET)) {
                                    return NOTHING;
                                } else {
                                    return TURN_RIGHT;
                                }
                            } else {
                                return NOTHING;
                            }
                        }
                    } else {
                        return TURN_RIGHT;
                    }
                } else {
                    if (nextBlocksLeft.contains(Terrain.MUD) || nextBlocksLeft.contains(Terrain.OIL_SPILL) || nextBlocksLeft.contains(Terrain.WALL) || nextBlocksLeft.contains(Terrain.TWEET)) {
                        if (hasPowerUp(PowerUps.LIZARD, myCar.powerups)) {
                            return USE_LIZARD;
                        } else {
                            if (nextBlocks.contains(Terrain.WALL) || nextBlocks.contains(Terrain.TWEET)) {
                                if (nextBlocksLeft.contains(Terrain.WALL) || nextBlocksLeft.contains(Terrain.TWEET)) {
                                    return NOTHING;
                                } else {
                                    return TURN_LEFT;
                                }
                            } else {
                                return NOTHING;
                            }
                        }
                    } else {
                        return TURN_LEFT;
                    }
                }
            }
        } */

        if (nextBlocks.contains(Terrain.MUD) || nextBlocks.contains(Terrain.OIL_SPILL) || nextBlocks.contains(Terrain.WALL) || nextBlocks.contains(Terrain.TWEET)) {
            if (hasPowerUp(PowerUps.LIZARD, myCar.powerups)) {
                return USE_LIZARD;
            } else if (myCar.position.lane == 1) {
                return TURN_RIGHT;
            } else if (myCar.position.lane == 2) {
                return TURN_RIGHT;
            } else if (myCar.position.lane == 3) {
                return TURN_LEFT;
            } else if (myCar.position.lane == 4) {
                return TURN_LEFT;
            }
        }

        //Basic aggression logic
        if (myCar.position.block < opponent.position.block) {
            if (hasPowerUp(PowerUps.EMP, myCar.powerups)) {
                return USE_EMP;
            }
        }

        if (myCar.position.block > opponent.position.block) {
            if (hasPowerUp(PowerUps.OIL, myCar.powerups)) {
                return USE_OIL;
            }
        }

        //Basic improvement logic
        if (!myCar.boosting && hasPowerUp(PowerUps.BOOST, myCar.powerups)) {
            return USE_BOOST;
        }

        //Accelerate first if going to slow
        if (myCar.speed < 8) {
            return ACCELERATE;
        }

        return NOTHING;

    }

    private Boolean hasPowerUp(PowerUps powerUpToCheck, PowerUps[] available) {
        for (PowerUps powerUp: available) {
            if (powerUp.equals(powerUpToCheck)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns map of blocks and the objects in the for the current lanes, returns
     * the amount of blocks that can be traversed at max speed.
     **/
    private List<Object> getBlocksInFront(int lane, int block, GameState gameState) {
        List<Lane[]> map = gameState.lanes;
        List<Object> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;

        Lane[] laneList = map.get(lane - 1);
        for (int i = max(block - startBlock, 0); i <= block - startBlock + 20; i++) {
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }

            blocks.add(laneList[i].terrain);

        }
        return blocks;
    }

    /* private List<Object> getBlocksInFrontLeft(int lane, int block, GameState gameState) {
        List<Lane[]> map = gameState.lanes;
        List<Object> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;

        if (lane != 1) {
            Lane[] laneList = map.get(lane - 2);
            for (int i = max(block - startBlock, 0); i <= block - startBlock + 20; i++) {
                if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                    break;
                }
    
                blocks.add(laneList[i].terrain);
    
            }
            return blocks;
        }
        return null;
    }

    private List<Object> getBlocksInFrontRight(int lane, int block, GameState gameState) {
        List<Lane[]> map = gameState.lanes;
        List<Object> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;

        if (lane != 4) {
            Lane[] laneList = map.get(lane);
            for (int i = max(block - startBlock, 0); i <= block - startBlock + 20; i++) {
                if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                    break;
                }
    
                blocks.add(laneList[i].terrain);
    
            }
            return blocks;
        }
        return null;
    } */

}