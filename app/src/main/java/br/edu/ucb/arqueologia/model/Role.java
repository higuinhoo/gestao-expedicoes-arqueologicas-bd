package br.edu.ucb.arqueologia.model;
public enum Role { ADMIN, CATALOGADOR, CONSULTA;
    public boolean writes() { return this != CONSULTA; }
}
