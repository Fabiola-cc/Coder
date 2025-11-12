package com.fmd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Reg {
    private List<String> TACinstructions; //Todas las instrucciones TAC
    private int availableRegistries; // Cantidad de registros disponibles
    private Map<String, String> registries; // Nombre de registro, variable que guarda
    private int freeRegistries;
    private Map<String, List<String>> describeDirection; // variable, direcciones donde se guarda

    public Reg(List<String> TACinstructions, int availableRegistries)
    {
        this.TACinstructions = TACinstructions;
        this.availableRegistries = availableRegistries; // Usada para tener el conteo de registros con espacio

        // Inicializar el mapa de registros según disponibilidad
        this.registries = new HashMap<String, String>(availableRegistries);

        this.describeDirection = new HashMap<>();
    }

    public void saveVar(String varToSave){
        this.describeDirection.put(varToSave, List.of(varToSave));
    }

    public String getReg(int numInstr, String varToSave){
        List<String> varPlaces = this.describeDirection.get(varToSave);
        if(varPlaces == null){ // Si no existe la variable se guarda en el descriptor
           saveVar(varToSave);
           varPlaces = this.describeDirection.get(varToSave);
        }
        if(varPlaces.size() > 1){ // Si la variable tiene un registro establecido se usa
            if (varPlaces.size() != 2) {
                for (int i = 2; i < varPlaces.size(); i++) { // aprovechamos a borrar los registros que tienen duplicados
                    this.registries.put(varPlaces.get(i), null);
                    varPlaces.remove(i);
                }
            }
            return varPlaces.get(1);
        }
        if (availableRegistries > 0){ // Si hay un registro disponible se usa
            for (int i = 1; i < availableRegistries; i++) {
                if (registries.get("R" + i) == null){
                    return "R" + i;
                }
            }
        } else {
            //TODO buscar elementos para quitar o mover
        }
        return null;
    }
}
