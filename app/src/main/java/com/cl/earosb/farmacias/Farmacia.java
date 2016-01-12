package com.cl.earosb.farmacias;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by earosb on 11-01-16.
 */
public class Farmacia {

    private String nombre;

    private String comuna;

    private String localidad;

    private String direccion;

    private String telefono;

    private String horaApertura;

    private String horaCierre;

    private Double longitud;

    private Double latitud;

    private String dia;

    private String fecha;

    public Farmacia() {
    }

    public Double getLongitud() {
        return longitud;
    }

    public void setLongitud(Double longitud) {
        this.longitud = longitud;
    }

    public void setComuna(String comuna) {
        this.comuna = comuna;
    }

    public Double getLatitud() {
        return latitud;
    }

    public void setLatitud(Double latitud) {
        this.latitud = latitud;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setLocalidad(String localidad) {
        this.localidad = localidad;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public boolean isValid() {
        return getLatitud() != null && getLongitud() != null;
    }

    public void setHoraApertura(String horaApertura) {
        this.horaApertura = horaApertura;
    }

    public void setHoraCierre(String horaCierre) {
        this.horaCierre = horaCierre;
    }

    public void setDia(String dia) {
        this.dia = dia;
    }

    public void setFecha(String fecha) {
        DateFormat formatOrigin = new SimpleDateFormat("yyyy-MM-dd");
        DateFormat formatCL = new SimpleDateFormat("dd-MM-yyyy");
        Date date = null;
        try {
            date = formatOrigin.parse(fecha);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        this.fecha = formatCL.format(date);
    }

    private String capitalize(final String line) {
        return Character.toUpperCase(line.charAt(0)) + line.substring(1);
    }

    @Override
    public String toString() {
        return "<p><b>" + capitalize(dia) + " " + fecha + ".</b></p>" +
                "<p><b>Dirección: </b>" + direccion + "</p>" +
                "<p><b>Teléfono: </b><a href='tel:" + telefono + "'>" + telefono + "</a></p>" +
                "<p><b>Horario de atención: </b>" + horaApertura + " hrs a " + horaCierre + " hrs</p>" +
                "<p>" + localidad + ", " + comuna + ".</p>";
    }
}
