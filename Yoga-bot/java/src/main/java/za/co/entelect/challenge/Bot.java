package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.PowerUps;
import za.co.entelect.challenge.enums.Terrain;

import java.util.*;

import static java.lang.Math.max;

import java.security.SecureRandom;

public class Bot {

    private static final int maxSpeed = 9;
    private List<Command> directionList = new ArrayList<>();

    private final Random random;

    private final static Command ACCELERATE = new AccelerateCommand();
    private final static Command LIZARD = new LizardCommand();
    private final static Command OIL = new OilCommand();
    private final static Command BOOST = new BoostCommand();
    private final static Command EMP = new EmpCommand();
    private final static Command FIX = new FixCommand();

    private final static Command TURN_RIGHT = new ChangeLaneCommand(1);
    private final static Command TURN_LEFT = new ChangeLaneCommand(-1);

    public Bot() {
        this.random = new SecureRandom();
        directionList.add(TURN_LEFT);
        directionList.add(TURN_RIGHT);
    }

    public Command run(GameState gameState) {
        Car myCar = gameState.player;
        Car opponent = gameState.opponent;

        //Mendeteksi obstacle di 3 lane terdekat
        List<Object> blocks = getBlocksInFront(myCar.position.lane, myCar.position.block, gameState);
        List<Object> rightBlocks = getBlocksInRight(myCar.position.lane, myCar.position.block, gameState);
        List<Object> leftBlocks = getBlocksInLeft(myCar.position.lane, myCar.position.block, gameState);

        //Fix first if too damaged to move
        if(myCar.damage >= 3) {
            return FIX;
        }
        //If my car too slow
        if(myCar.speed <= 3){
            return ACCELERATE;
        }

        //Pemilihan command untuk menghindari obstacle
        if (blocks.contains(Terrain.MUD) || blocks.contains(Terrain.WALL)){
            if(hasPowerUp(PowerUps.LIZARD, myCar.powerups)) {
                return LIZARD;
            }
            else {
                if (rightBlocks.contains(Terrain.MUD) || rightBlocks.contains(Terrain.WALL)){
                    return TURN_LEFT;
                }
                if(leftBlocks.contains(Terrain.MUD) || leftBlocks.contains(Terrain.WALL)){
                    return TURN_RIGHT;
                }
            }
            return TURN_RIGHT;
        }

        //Move if get 1 or 4 lane
        if(myCar.position.lane == 1 && !(rightBlocks.contains(Terrain.MUD) || rightBlocks.contains(Terrain.WALL))){
            return TURN_RIGHT;
        }
        if(myCar.position.lane == 4 && !(leftBlocks.contains(Terrain.MUD) || leftBlocks.contains(Terrain.WALL))){
            return TURN_LEFT;
        }

        //Menggunakan boost jika sudah didapatkan
        if (hasPowerUp(PowerUps.BOOST, myCar.powerups)) {
            return BOOST;
        }

        return ACCELERATE;
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

        if(lane == 4){
            return blocks;
        }
        if(lane == -1){
            return blocks;
        }

        Lane[] laneList = map.get(lane - 1);
        for (int i = max(block - startBlock, 0); i <= block - startBlock + gameState.player.speed; i++) {
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }

            blocks.add(laneList[i].terrain);

        }
        return blocks;
    }

    private List<Object> getBlocksInRight(int lane, int block, GameState gameState) {
        List<Lane[]> map = gameState.lanes;
        List<Object> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;

        if(lane == 4){
            return blocks;
        }

        Lane[] laneList = map.get(lane - 1);
        for (int i = max(block - startBlock, 0); i <= block - startBlock + gameState.player.speed; i++) {
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }

            blocks.add(laneList[i].terrain);

        }
        return blocks;
    }

    private List<Object> getBlocksInLeft(int lane, int block, GameState gameState) {
        List<Lane[]> map = gameState.lanes;
        List<Object> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;

        if(lane == -1){
            return blocks;
        }

        Lane[] laneList = map.get(lane - 1);
        for (int i = max(block - startBlock, 0); i <= block - startBlock + gameState.player.speed; i++) {
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }

            blocks.add(laneList[i].terrain);

        }
        return blocks;
    }

}
