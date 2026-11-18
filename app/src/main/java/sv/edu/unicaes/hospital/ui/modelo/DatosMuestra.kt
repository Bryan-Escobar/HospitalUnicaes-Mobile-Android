package sv.edu.unicaes.hospital.ui.modelo

import sv.edu.unicaes.hospital.domain.model.EstadoAmbulancia
import sv.edu.unicaes.hospital.domain.model.EstadoCita
import sv.edu.unicaes.hospital.domain.model.TipoHabitacion

/**
 * Datos de ejemplo del handoff de diseño (§8), palabra por palabra, para maquetar las
 * pantallas y sus previews mientras no existen los endpoints. Respeta la capacidad fija:
 * 5 VIP + 6 semiprivadas = 11 habitaciones y 17 camas; 1 ambulancia.
 */
object DatosMuestra {
    // Recepción ------------------------------------------------------------------------------

    val turnoRecepcion =
        TurnoUi(
            dia = "Hoy",
            horario = "7:00 a. m. – 3:00 p. m.",
            area = "Recepción",
            tipo = "Recepcionista",
            enCurso = true,
        )

    val solicitudAmbulancia =
        SolicitudAmbulanciaUi(
            id = "amb-1",
            paciente = "Luis Martínez",
            hora = "10:12 a. m.",
            hace = "hace 1 min",
            tipo = "Dolor de pecho",
            ubicacion = "Col. Escalón, San Salvador (GPS ±20 m)",
            descripcion = "dolor fuerte desde hace 20 minutos",
        )

    val ocupacion =
        listOf(
            OcupacionUi(TipoHabitacion.SEMIPRIVADA, ocupadas = 9, total = 12),
            OcupacionUi(TipoHabitacion.VIP, ocupadas = 3, total = 5),
        )

    val citasHoy =
        listOf(
            CitaUi("c-1", "10:30", "Luis Martínez", "Dra. Rivas", "Control de alergia", EstadoCita.PROGRAMADA),
        )

    val inicioRecepcion =
        InicioRecepcionUi(
            turno = turnoRecepcion,
            solicitudAmbulancia = solicitudAmbulancia,
            solicitudesAmbulancia = 1,
            laboratorioPendiente = 7,
            ocupacion = ocupacion,
            citasHoyTotal = 18,
            citasHoy = citasHoy,
        )

    val pacientes =
        listOf(
            PacienteUi("p-1", "Luis Martínez", "01234567-8", etiqueta = "HAB-204 · A", hospitalizado = true),
            PacienteUi("p-2", "María López", "04512378-9", etiqueta = "Emergencia"),
            PacienteUi("p-3", "Elena Cruz", "03344556-1"),
            PacienteUi("p-4", "Mario Ruiz", "05566778-2"),
            PacienteUi("p-5", "Rosa Méndez", "02233445-6", etiqueta = "VIP-302", hospitalizado = true),
        )

    val asignacionLuis =
        AsignacionCamaUi(
            habitacion = "HAB-204",
            cama = "A",
            tipo = TipoHabitacion.SEMIPRIVADA,
            desde = "19 oct",
            ingreso = "19 oct · 16:40",
            diasHospitalizado = 2,
            medicoTratante = "Dr. Carlos Molina",
        )

    val fichaLuis =
        FichaPacienteUi(
            paciente = pacientes.first(),
            edad = 34,
            telefono = "7000-1111",
            contactoEmergencia = "Ana Martínez · 7000-2222",
            asignacion = asignacionLuis,
            citas =
                listOf(
                    CitaUi(
                        "c-1",
                        "10:30",
                        "Luis Martínez",
                        "Dra. Rivas · Alergista",
                        "Control de alergia",
                        EstadoCita.PROGRAMADA,
                        fecha = "Jue 22 oct",
                    ),
                    CitaUi(
                        "c-0",
                        "10:00",
                        "Luis Martínez",
                        "Dra. Rivas",
                        "Control de alergia",
                        EstadoCita.ATENDIDA,
                        fecha = "14 oct",
                    ),
                ),
        )

    val diasSemana = listOf("Lun 19", "Mar 20", "Mié 21", "Jue 22", "Vie 23")

    val agendasDelDia =
        listOf(
            AgendaDoctorUi(
                doctor = "Dra. Ana Rivas",
                especialidad = "Alergista",
                horario = "8:00 a. m. – 2:00 p. m.",
                citas =
                    listOf(
                        CitaUi(
                            "c-1",
                            "10:30",
                            "Luis Martínez",
                            "Dra. Rivas",
                            "Control de alergia",
                            EstadoCita.PROGRAMADA,
                        ),
                        CitaUi("c-2", "11:30", "Elena Cruz", "Dra. Rivas", "Prueba cutánea", EstadoCita.PROGRAMADA),
                    ),
            ),
            AgendaDoctorUi(
                doctor = "Dr. Carlos Molina",
                especialidad = "General",
                horario = "7:00 a. m. – 3:00 p. m.",
                citas = listOf(CitaUi("c-3", "09:00", "Pedro Alas", "Dr. Molina", "Fiebre", EstadoCita.ATENDIDA)),
                horasLibres = listOf("13:00"),
            ),
        )

    private fun semiprivada(
        codigo: String,
        a: String?,
        b: String?,
    ) = HabitacionUi(
        codigo,
        TipoHabitacion.SEMIPRIVADA,
        listOf(CamaUi("$codigo-A", "A", a), CamaUi("$codigo-B", "B", b)),
    )

    private fun vip(
        codigo: String,
        ocupante: String?,
    ) = HabitacionUi(codigo, TipoHabitacion.VIP, listOf(CamaUi(codigo, "", ocupante)))

    val planta2 =
        listOf(
            semiprivada("HAB-201", "L. Gómez", "J. Díaz"),
            semiprivada("HAB-202", "R. Paz", null),
            semiprivada("HAB-203", "C. Ríos", "M. Sol"),
            semiprivada("HAB-204", "L. Martínez", "A. Vega"),
            semiprivada("HAB-205", "E. Lara", null),
            semiprivada("HAB-206", "T. Mora", null),
        )

    val planta3 =
        listOf(
            vip("VIP-301", "J. Ortiz"),
            vip("VIP-302", "R. Méndez"),
            vip("VIP-303", null),
            vip("VIP-304", "F. Luna"),
            vip("VIP-305", null),
        )

    val pacienteIngreso = PacienteUi("p-2", "María López", "04512378-9", etiqueta = "Ingreso por emergencia")

    val turnosPersonal =
        listOf(
            AreaTurnosUi(
                area = "Consulta externa",
                turnos =
                    listOf(
                        TurnoPersonalUi("Dra. Ana Rivas", "Consulta externa", "8:00 a. m. – 2:00 p. m.", "Alergista"),
                    ),
            ),
            AreaTurnosUi(
                area = "Emergencia",
                turnos =
                    listOf(
                        TurnoPersonalUi(
                            "Dr. Jorge Peña",
                            "Emergencia",
                            "6:00 a. m. – 6:00 p. m.",
                            "Emergencia",
                            enCurso = true,
                        ),
                        TurnoPersonalUi("María Gómez", "Emergencia", "3:00 p. m. – 11:00 p. m.", "Enfermería"),
                    ),
            ),
        )

    val ambulancia = AmbulanciaUi(estado = EstadoAmbulancia.DISPONIBLE, solicitudes = listOf(solicitudAmbulancia))

    val bitacora =
        listOf(
            ServicioBitacoraUi(
                "b-1",
                "21 oct",
                "10:14 a. m. – 11:02 a. m.",
                "Dolor de pecho",
                "L. Martínez",
                "48 min",
                EstadoServicio.FINALIZADO,
            ),
            ServicioBitacoraUi(
                "b-2",
                "19 oct",
                "10:40 p. m. – 11:35 p. m.",
                "Accidente de tránsito",
                duracion = "55 min",
                estado = EstadoServicio.FINALIZADO,
            ),
            ServicioBitacoraUi(
                "b-3",
                "18 oct",
                "7:10 a. m.",
                "Dificultad respiratoria",
                estado = EstadoServicio.RECHAZADA,
            ),
            ServicioBitacoraUi(
                "b-4",
                "15 oct",
                "8:00 a. m. – 12:00 p. m.",
                "Mantenimiento preventivo",
                estado = EstadoServicio.EN_MANTENIMIENTO,
            ),
        )

    val detalleServicio =
        DetalleServicioUi(
            fecha = "21 oct",
            despacho = "Recepción (Karla)",
            observaciones = "paciente estable al llegar.",
        )

    // Doctor ---------------------------------------------------------------------------------

    val turnoDoctor =
        TurnoUi(
            dia = "Hoy",
            horario = "6:00 a. m. – 6:00 p. m.",
            area = "Emergencias",
            tipo = "Emergencia",
            enCurso = true,
        )

    val agendaDoctor =
        AgendaUi(
            turno = turnoDoctor,
            dias = diasSemana,
            diaSeleccionado = 2,
            citas =
                listOf(
                    CitaUi("d-1", "08:00", "Ana Vega", "Dr. Peña", "Herida en mano", EstadoCita.ATENDIDA),
                    CitaUi("d-2", "10:00", "María López", "Dr. Peña", "Dolor abdominal", EstadoCita.PROGRAMADA),
                    CitaUi("d-3", "11:30", "Tomás Mora", "Dr. Peña", "Control post-ingreso", EstadoCita.PROGRAMADA),
                    CitaUi("d-4", "13:00", "Carla Ríos", "Dr. Peña", "Fiebre", EstadoCita.CANCELADA),
                ),
        )

    val detalleCita =
        DetalleCitaUi(
            cita = agendaDoctor.citas[1].copy(fecha = "Mié 21 oct · 10:00 a. m."),
            edad = 41,
            dui = "04512378-9",
            alergias = null,
            // La agenda aún es de muestra: este id no existe en la API.
            pacienteId = "p-2",
        )

    val turnosDoctorSemana =
        listOf(
            TurnoUi("Lun 19", "6:00 a. m. – 6:00 p. m.", "Emergencias", terminado = true),
            TurnoUi("Mar 20", "", "", sinTurno = true),
            TurnoUi("Mié 21", "6:00 a. m. – 6:00 p. m.", "Emergencias", enCurso = true),
            TurnoUi("Vie 23", "6:00 p. m. – 6:00 a. m.", "Emergencias · nocturno", tipo = "Emergencia"),
            TurnoUi("Sáb 24 – Dom 25", "", "", sinTurno = true),
        )

    val turnosDoctorSiguiente =
        listOf(
            TurnoUi("Lun 26", "6:00 a. m. – 6:00 p. m.", "Emergencias"),
            TurnoUi("Mié 28", "6:00 a. m. – 6:00 p. m.", "Emergencias"),
        )

    val perfilDoctor =
        PerfilUi(
            nombre = "Dr. Jorge Peña",
            rolYTipo = "Doctor · Médico de emergencia",
            correo = "jorge.pena@hospital.edu",
            telefono = "7000-0000",
        )
}
