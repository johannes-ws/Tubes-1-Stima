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
        List<Object> blocks15 = getBlocksInFront(myCar.position.lane, myCar.position.block, gameState,15);
        List<Object> blocks = getBlocksInFront(myCar.position.lane, myCar.position.block, gameState,9);
        List<Object> rightBlocks = getBlocksInRight(myCar.position.lane, myCar.position.block, gameState, myCar.speed);
        List<Object> leftBlocks = getBlocksInLeft(myCar.position.lane, myCar.position.block, gameState, myCar.speed);

        //Memperbaiki mobil segera ketika damage sudah lebih dari samadengan 2
        if(myCar.damage > 1) {
            return FIX;
        }

        //Menggunakan boost jika sudah didapatkan
        if (hasPowerUp(PowerUps.BOOST, myCar.powerups) && myCar.damage == 0 && !(blocks15.contains(Terrain.MUD) || blocks15.contains(Terrain.WALL) || blocks15.contains(Terrain.OIL_SPILL))) {
            return BOOST;
        }

        //Move if get 1 or 4 lane
        if (myCar.speed < 5){
            return ACCELERATE;
        }

        //Pemilihan command untuk menghindari obstacle (Mud, Wall, Oil, Truck belum)
        if (blocks.contains(Terrain.MUD) || blocks.contains(Terrain.WALL) || blocks.contains(Terrain.OIL_SPILL)){
            if(hasPowerUp(PowerUps.LIZARD, myCar.powerups)) {
                return LIZARD;
            }
            else {
                if (myCar.position.lane != 1 && (rightBlocks.contains(Terrain.MUD) || rightBlocks.contains(Terrain.WALL) || rightBlocks.contains(Terrain.OIL_SPILL))){
                    return TURN_LEFT;
                }
                if(myCar.position.lane != 4 && (leftBlocks.contains(Terrain.MUD) || leftBlocks.contains(Terrain.WALL) || leftBlocks.contains(Terrain.OIL_SPILL))){
                    return TURN_RIGHT;
                }
            }
            return ACCELERATE;
        }


        if(myCar.position.lane == 1 && !(rightBlocks.contains(Terrain.MUD) || rightBlocks.contains(Terrain.WALL))){
            return TURN_RIGHT;
        }
        if(myCar.position.lane == 4 && !(leftBlocks.contains(Terrain.MUD) || leftBlocks.contains(Terrain.WALL))){
            return TURN_LEFT;
        }

        //Menggunakan EMP jika opponent ada di depan myCar dan berada di lane yang sama atau bersebelahan
        if(Math.abs(myCar.position.lane - opponent.position.lane) < 2 && myCar.position.block < opponent.position.block){
            return EMP;
        }

        //Menggunakan Tweet jika sudah punya dan kecepatan 9
        if(myCar.speed > 8 && hasPowerUp(PowerUps.TWEET, myCar.powerups)){
            int lane = opponent.position.lane;
            int block = opponent.position.block + opponent.speed + 3;
            Command USE_TWEET = new TweetCommand(lane, block);
            return USE_TWEET;
        }

        //Menggunakan OIL di lane tengah jika kecepatan 9
        if (myCar.speed > 8 && (myCar.position.lane == 2 || myCar.position.lane == 3)){
            return OIL;
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

    private List<Object> getBlocksInFront(int lane, int block, GameState gameState, int speed) {
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
        for (int i = max(block - startBlock, 0); i <= block - startBlock + 15; i++) {
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }

            blocks.add(laneList[i].terrain);

        }
        return blocks;
    }

    private List<Object> getBlocksInRight(int lane, int block, GameState gameState, int speed) {
        List<Lane[]> map = gameState.lanes;
        List<Object> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;

        if(lane == 4){
            return blocks;
        }

        Lane[] laneList = map.get(lane - 1);
        for (int i = max(block - startBlock, 0); i <= block - startBlock + speed; i++) {
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }

            blocks.add(laneList[i].terrain);

        }
        return blocks;
    }

    private List<Object> getBlocksInLeft(int lane, int block, GameState gameState, int speed) {
        List<Lane[]> map = gameState.lanes;
        List<Object> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;

        if(lane == -1){
            return blocks;
        }

        Lane[] laneList = map.get(lane - 1);
        for (int i = max(block - startBlock, 0); i <= block - startBlock + speed; i++) {
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }

            blocks.add(laneList[i].terrain);

        }
        return blocks;
    }

}