package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.PowerUps;
import za.co.entelect.challenge.enums.Terrain;

import java.util.*;

import static java.lang.Math.max;

public class Bot {

    private static final int minSpeed = 0;
    private static final int speed1 = 3;
    private static final int initialSpeed = 5;
    private static final int speed2 = 6;
    private static final int speed3 = 8;
    private static final int maxSpeed = 9;
    private static final int boostSpeed = 15;

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

    public Bot() {

    }

    public Command run(GameState gameState) {
        Car myCar = gameState.player;
        Car opponent = gameState.opponent;

        List<Object> blocks = getBlocksInFront(myCar.position.lane, myCar.position.block, gameState);
        List<Object> blocksLeft = getBlocksInFrontLeft(myCar.position.lane, myCar.position.block, gameState);
        List<Object> blocksRight = getBlocksInFrontRight(myCar.position.lane, myCar.position.block, gameState);
        List<Object> blocksBehind = getBlocksInBehind(myCar.position.lane, myCar.position.block, gameState);
        List<Object> nextBlocks = blocks.subList(0,myCar.speed);

        // Benerin mobil dulu
        if (myCar.damage > 1) {
            return FIX;
        }
        
        // Jaga-jaga kalo mobil berhenti
        if (myCar.speed == minSpeed) {
            return ACCELERATE;
        }

        // Selamatkan diri
        if (isObstaclePresent(nextBlocks)) {
            if (myCar.position.lane != 1 && myCar.position.lane != 4) {
                List<Object> nextBlocksLeft = blocksLeft.subList(0,myCar.speed);
                List<Object> nextBlocksRight = blocksRight.subList(0,myCar.speed);
                if (isObstaclePresent(nextBlocksLeft)) {
                    if (isObstaclePresent(nextBlocksRight)) {
                        if (hasPowerUp(PowerUps.LIZARD, myCar.powerups)) {
                            return USE_LIZARD;
                        } else {
                            if (nextBlocks.contains(Terrain.WALL)) {
                                if (nextBlocksLeft.contains(Terrain.WALL)) {
                                    if (nextBlocksRight.contains(Terrain.WALL)) {
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
                    List<Object> nextBlocksRight = blocksRight.subList(0,myCar.speed);
                    if (isObstaclePresent(nextBlocksRight)) {
                        if (hasPowerUp(PowerUps.LIZARD, myCar.powerups)) {
                            return USE_LIZARD;
                        } else {
                            if (nextBlocks.contains(Terrain.WALL)) {
                                if (nextBlocksRight.contains(Terrain.WALL)) {
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
                    List<Object> nextBlocksLeft = blocksLeft.subList(0,myCar.speed);
                    if (isObstaclePresent(nextBlocksLeft)) {
                        if (hasPowerUp(PowerUps.LIZARD, myCar.powerups)) {
                            return USE_LIZARD;
                        } else {
                            if (nextBlocks.contains(Terrain.WALL)) {
                                if (nextBlocksLeft.contains(Terrain.WALL)) {
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
        }

        // Ambil powerups
        if (!isPowerupPresent(nextBlocks)) {
            if (myCar.position.lane != 1 && myCar.position.lane != 4) {
                List<Object> nextBlocksLeft = blocksLeft.subList(0,myCar.speed);
                List<Object> nextBlocksRight = blocksRight.subList(0,myCar.speed);
                if (!isObstaclePresent(nextBlocksLeft) && isPowerupPresent(nextBlocksLeft)) {
                    return TURN_LEFT;
                } else if (!isObstaclePresent(nextBlocksRight) && isPowerupPresent(nextBlocksRight)) {
                    return TURN_RIGHT;
                }
            } else {
                if (myCar.position.lane == 1) {
                    List<Object> nextBlocksRight = blocksRight.subList(0,myCar.speed);
                    if (!isObstaclePresent(nextBlocksRight) && isPowerupPresent(nextBlocksRight)) {
                        return TURN_RIGHT;
                    }
                } else {
                    List<Object> nextBlocksLeft = blocksLeft.subList(0,myCar.speed);
                    if (!isObstaclePresent(nextBlocksLeft) && isPowerupPresent(nextBlocksLeft)) {
                        return TURN_LEFT;
                    }
                }
            }
        }

        // Menyalip musuh
        if (myCar.position.lane == opponent.position.lane && myCar.position.block < opponent.position.block) {
            if (myCar.position.block+boostSpeed >= opponent.position.block+opponent.speed) {
                if (myCar.position.lane != 1 && myCar.position.lane != 4) {
                    List<Object> nextBlocksLeft = blocksLeft.subList(0,myCar.speed);
                    List<Object> nextBlocksRight = blocksRight.subList(0,myCar.speed);
                    if (isObstaclePresent(nextBlocksLeft)) {
                        if (!isObstaclePresent(nextBlocksRight)) {
                            return TURN_RIGHT;
                        }
                    } else {
                        return TURN_LEFT;
                    }
                } else {
                    if (myCar.position.lane == 1) {
                        List<Object> nextBlocksRight = blocksRight.subList(0,myCar.speed);
                        if (!isObstaclePresent(nextBlocksRight)) {
                            return TURN_RIGHT;
                        }
                    } else {
                        List<Object> nextBlocksLeft = blocksLeft.subList(0,myCar.speed);
                        if (!isObstaclePresent(nextBlocksLeft)) {
                            return TURN_LEFT;
                        }
                    }
                }
            }
        }

        // Serang musuh
        if (myCar.position.lane == 1) {
            if ((myCar.position.block < opponent.position.block && myCar.position.lane == opponent.position.lane) || (myCar.position.block <= opponent.position.block && myCar.position.lane == opponent.position.lane - 1)) {
                if (hasPowerUp(PowerUps.EMP, myCar.powerups)) {
                    return USE_EMP;
                }
            }
        } else if (myCar.position.lane == 4) {
            if ((myCar.position.block < opponent.position.block && myCar.position.lane == opponent.position.lane) || (myCar.position.block <= opponent.position.block && myCar.position.lane == opponent.position.lane + 1)) {
                if (hasPowerUp(PowerUps.EMP, myCar.powerups)) {
                    return USE_EMP;
                }
            }
        } else if ((myCar.position.block < opponent.position.block && myCar.position.lane == opponent.position.lane) || (myCar.position.block <= opponent.position.block && myCar.position.lane == opponent.position.lane + 1) || (myCar.position.block <= opponent.position.block && myCar.position.lane == opponent.position.lane - 1)) {
            if (hasPowerUp(PowerUps.EMP, myCar.powerups)) {
                return USE_EMP;
            }
        }

        if (hasPowerUp(PowerUps.TWEET, myCar.powerups)) {
            int lane = opponent.position.lane;
            int block = opponent.position.block + opponent.speed + 3;
            Command USE_TWEET = new TweetCommand(lane, block); 
            return USE_TWEET;
        }

        if (myCar.position.block > opponent.position.block && (!isObstaclePresent(blocksBehind) || myCar.position.lane == opponent.position.lane)) {
            if (hasPowerUp(PowerUps.OIL, myCar.powerups)) {
                return USE_OIL;
            }
        }

        // Sebelum boost harus 0 damage
        if (hasPowerUp(PowerUps.BOOST, myCar.powerups) && myCar.damage == 1) {
            return FIX;
        }
        
        // Boost mobil
        nextBlocks = blocks.subList(0,boostSpeed);
        if (!isObstaclePresent(nextBlocks)) {
            if (myCar.boostCounter <= 1 && hasPowerUp(PowerUps.BOOST, myCar.powerups) && myCar.damage == 0) {
                return USE_BOOST;
            }
        }

        // Percepat mobil
        if (myCar.speed == speed1) {
            nextBlocks = blocks.subList(0,speed2);
            if (!isObstaclePresent(nextBlocks)) {
                return ACCELERATE;
            }
        } else if (myCar.speed == initialSpeed) {
            nextBlocks = blocks.subList(0,speed2);
            if (!isObstaclePresent(nextBlocks)) {
                return ACCELERATE;
            }
        } else if (myCar.speed == speed2) {
            nextBlocks = blocks.subList(0,speed3);
            if (!isObstaclePresent(nextBlocks)) {
                return ACCELERATE;
            }
        } else if (myCar.speed == speed3) {
            nextBlocks = blocks.subList(0,maxSpeed);
            if (!isObstaclePresent(nextBlocks)) {
                return ACCELERATE;
            }
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

    private List<Object> getBlocksInFront(int lane, int block, GameState gameState) {
        List<Lane[]> map = gameState.lanes;
        List<Object> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;

        Lane[] laneList = map.get(lane - 1);
        for (int i = max(block - startBlock + 1, 0); i <= block - startBlock + 20; i++) {
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }

            blocks.add(laneList[i].terrain);
        }
        return blocks;
    }

    private List<Object> getBlocksInFrontLeft(int lane, int block, GameState gameState) {
        List<Lane[]> map = gameState.lanes;
        List<Object> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;

        if (lane == 1) {
            return Collections.emptyList();
        }

        Lane[] laneList = map.get(lane - 2);
        for (int i = max(block - startBlock, 0); i <= block - startBlock + 20; i++) {
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }

            blocks.add(laneList[i].terrain);
        }
        return blocks;
    }

    private List<Object> getBlocksInFrontRight(int lane, int block, GameState gameState) {
        List<Lane[]> map = gameState.lanes;
        List<Object> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;

        if (lane == 4) {
            return Collections.emptyList();
        }

        Lane[] laneList = map.get(lane);
        for (int i = max(block - startBlock, 0); i <= block - startBlock + 20; i++) {
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }

            blocks.add(laneList[i].terrain);
        }
        return blocks;
    }

    private List<Object> getBlocksInBehind(int lane, int block, GameState gameState) {
        List<Lane[]> map = gameState.lanes;
        List<Object> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;

        Lane[] laneList = map.get(lane - 1);
        for (int i = max(block - startBlock - 5, 0); i < block - startBlock; i++) {
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }

            blocks.add(laneList[i].terrain);
        }
        return blocks;
    }

    private Boolean isObstaclePresent(List<Object> blocks) {
        // Mengembalikan true jika ada obstacle di suatu lane, pengecekan dimulai dari block paling belakang
        // yang terlihat hingga sebanyak numOfBlocks
        return blocks.contains(Terrain.MUD) || blocks.contains(Terrain.OIL_SPILL) || blocks.contains(Terrain.WALL);
    }

    private Boolean isPowerupPresent(List<Object> blocks) {
        // Mengembalikan true jika ada obstacle di suatu lane, pengecekan dimulai dari block paling belakang
        // yang terlihat hingga sebanyak numOfBlocks
        return blocks.contains(Terrain.OIL_POWER) || blocks.contains(Terrain.BOOST) || blocks.contains(Terrain.LIZARD) || blocks.contains(Terrain.TWEET) || blocks.contains(Terrain.EMP);
    }
    
}