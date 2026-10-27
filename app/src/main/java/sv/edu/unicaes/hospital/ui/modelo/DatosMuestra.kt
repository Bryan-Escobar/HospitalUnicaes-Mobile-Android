package sv.edu.unicaes.hospital.ui.modelo

import sv.edu.unicaes.hospital.domain.model.EstadoAmbulancia
import sv.edu.unicaes.hospital.domain.model.EstadoCita
import sv.edu.unicaes.hospital.domain.model.EstadoLaboratorio
import sv.edu.unicaes.hospital.domain.model.TipoHabitacion

/**
 * Datos de ejemplo del handoff de diseño (§8), palabra por palabra, para maquetar las
 * pantallas y sus previews mientras no existen los endpoints. Respeta la capacidad fija:
 * 5 VIP + 6 semiprivadas = 11 habitaciones y 17 camas; 1 ambulancia.
 */
object DatosMuestra {
    // Recepción ------------------------------------------------------------------------------

    val turnoRecepcion =
        TurnoUi(dia = "Hoy", horario = "07:00–15:00", area = "Recepción", tipo = "Recepcionista", enCurso = true)

    val solicitudAmbulancia =
        SolicitudAmbulanciaUi(
            id = "amb-1",
            paciente = "Luis Martínez",
            hora = "10:12",
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
            solicitudesCitas = 3,
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
                horario = "08:00–14:00",
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
                horario = "07:00–15:00",
                citas = listOf(CitaUi("c-3", "09:00", "Pedro Alas", "Dr. Molina", "Fiebre", EstadoCita.ATENDIDA)),
                horasLibres = listOf("13:00"),
            ),
        )

    /** Horas de la Dra. Rivas el jueves; las ocupadas se muestran deshabilitadas. */
    val horasCita = listOf("08:00", "08:30", "09:00", "10:30", "11:00", "12:30")
    val horasOcupadas = setOf("09:00")

    val solicitudesCitas =
        listOf(
            SolicitudCitaUi(
                "s-1",
                "Luis Martínez",
                "hace 5 min",
                "molestias en la garganta y tos",
                "Medicina general",
                "Lun 26 · mañana",
            ),
            SolicitudCitaUi(
                "s-2",
                "Elena Cruz",
                "hace 40 min",
                "revisión de prueba cutánea",
                "Alergista",
                "Cualquier día · tarde",
            ),
            SolicitudCitaUi(
                "s-3",
                "Pedro Alas",
                "ayer",
                "no sé qué especialista necesito",
                "No sabe",
                "Mié 28 · mañana",
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
            DiaTurnosUi(
                dia = "Mié 21",
                turnos =
                    listOf(
                        TurnoPersonalUi("Dra. Ana Rivas", "Consulta externa", "08:00–14:00", "Alergista"),
                        TurnoPersonalUi("Dr. Jorge Peña", "Emergencias", "06:00–18:00", "Emergencia"),
                    ),
            ),
        )

    val ambulancia = AmbulanciaUi(estado = EstadoAmbulancia.DISPONIBLE, solicitudes = listOf(solicitudAmbulancia))

    val bitacora =
        listOf(
            ServicioBitacoraUi(
                "b-1",
                "21 oct",
                "10:14 – 11:02",
                "Dolor de pecho",
                "L. Martínez",
                "48 min",
                EstadoServicio.FINALIZADO,
            ),
            ServicioBitacoraUi(
                "b-2",
                "19 oct",
                "22:40 – 23:35",
                "Accidente de tránsito",
                duracion = "55 min",
                estado = EstadoServicio.FINALIZADO,
            ),
            ServicioBitacoraUi("b-3", "18 oct", "07:10", "Dificultad respiratoria", estado = EstadoServicio.RECHAZADA),
            ServicioBitacoraUi(
                "b-4",
                "15 oct",
                "08:00 – 12:00",
                "Mantenimiento preventivo",
                estado = EstadoServicio.EN_MANTENIMIENTO,
            ),
        )

    val detalleServicio =
        DetalleServicioUi(
            fecha = "21 oct",
            despacho = "Recepción (Karla)",
            destino = "Emergencias HUC",
            kilometraje = 12,
            observaciones = "paciente estable al llegar.",
        )

    val ordenOrina =
        OrdenLabUi(
            id = "o-1042",
            numero = 1042,
            examen = "Examen general de orina",
            paciente = "Luis Martínez",
            dui = "01234567-8",
            doctor = "Dr. Carlos Molina",
            hora = "hace 2 min",
            estado = EstadoLaboratorio.EN_PROCESO,
            indicaciones = "primera orina de la mañana",
            ordenada = "21 oct 10:41",
            enProcesoDesde = "10:55",
            nueva = true,
        )

    val colaLaboratorio =
        listOf(
            ordenOrina.copy(paciente = "L. Martínez", doctor = "Dr. Molina", estado = EstadoLaboratorio.PENDIENTE),
            OrdenLabUi(
                "o-1041",
                1041,
                "Hemograma completo",
                "M. López",
                "04512378-9",
                "Dr. Peña",
                "09:40",
                EstadoLaboratorio.PENDIENTE,
            ),
            OrdenLabUi(
                "o-1040",
                1040,
                "Química sanguínea",
                "E. Cruz",
                "03344556-1",
                "Dr. Molina",
                "09:15",
                EstadoLaboratorio.PENDIENTE,
            ),
            OrdenLabUi(
                "o-1039",
                1039,
                "Prueba cutánea",
                "P. Alas",
                "05566778-2",
                "Dra. Rivas",
                "08:50",
                EstadoLaboratorio.PENDIENTE,
            ),
            OrdenLabUi(
                "o-1038",
                1038,
                "Perfil lipídico",
                "R. Méndez",
                "02233445-6",
                "Dr. Molina",
                "ayer",
                EstadoLaboratorio.PENDIENTE,
            ),
        )

    /** Conteos por pestaña de la cola: Pendiente 7 · En Proceso 4. */
    val conteoLaboratorio = mapOf(EstadoLaboratorio.PENDIENTE to 7, EstadoLaboratorio.EN_PROCESO to 4)

    val perfilRecepcion =
        PerfilUi(
            nombre = "Karla Flores",
            rolYTipo = "Recepcionista/Asistente · Asistente",
            correo = "karla.flores@hospital.edu",
            telefono = "7000-3333",
        )

    // Doctor ---------------------------------------------------------------------------------

    val turnoDoctor =
        TurnoUi(dia = "Hoy", horario = "06:00–18:00", area = "Emergencias", tipo = "Emergencia", enCurso = true)

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
            cita = agendaDoctor.citas[1].copy(fecha = "Mié 21 oct · 10:00"),
            edad = 41,
            dui = "04512378-9",
            alergias = null,
        )

    val misPacientes =
        listOf(
            PacienteUi("p-2", "María López", "04512378-9", etiqueta = "Emergencia", ultimaVisita = "Hoy"),
            PacienteUi(
                "p-6",
                "Tomás Mora",
                "06677889-3",
                etiqueta = "HAB-206 · A",
                hospitalizado = true,
                ultimaVisita = "20 oct",
            ),
            PacienteUi("p-7", "Ana Vega", "07788990-4", ultimaVisita = "Hoy"),
            PacienteUi("p-8", "Carla Ríos", "08899001-5", ultimaVisita = "15 oct"),
        )

    val expedienteTomas =
        ExpedienteUi(
            paciente = misPacientes[1],
            edad = 52,
            alergias = "Ibuprofeno",
            hospitalizacion = "HAB-206 · Cama A · desde 20 oct",
            consultas =
                listOf(
                    ConsultaUi("20 oct", "Apendicitis aguda", "Dr. Jorge Peña", recetas = 1, ordenes = 2),
                    ConsultaUi("3 jun", "Gastritis", "Dr. Carlos Molina", recetas = 0, ordenes = 0),
                ),
            laboratorio =
                listOf(
                    OrdenLabUi(
                        "o-1030",
                        1030,
                        "Hemograma completo",
                        "Tomás Mora",
                        "06677889-3",
                        "Dr. Peña",
                        "20 oct",
                        EstadoLaboratorio.COMPLETADO,
                    ),
                    OrdenLabUi(
                        "o-1036",
                        1036,
                        "Química sanguínea",
                        "Tomás Mora",
                        "06677889-3",
                        "Dr. Peña",
                        "21 oct",
                        EstadoLaboratorio.EN_PROCESO,
                    ),
                ),
            telefono = "7000-4444",
            contactoEmergencia = "Lucía Mora · 7000-5555",
        )

    val tiposExamen =
        listOf(
            "Química sanguínea",
            "Hemograma completo",
            "Examen general de orina",
            "Perfil lipídico",
            "Prueba cutánea",
        )

    val turnosDoctorSemana =
        listOf(
            TurnoUi("Lun 19", "06:00–18:00", "Emergencias", terminado = true),
            TurnoUi("Mar 20", "", "", sinTurno = true),
            TurnoUi("Mié 21", "06:00–18:00", "Emergencias", enCurso = true),
            TurnoUi("Vie 23", "18:00–06:00", "Emergencias · nocturno", tipo = "Emergencia"),
            TurnoUi("Sáb 24 – Dom 25", "", "", sinTurno = true),
        )

    val turnosDoctorSiguiente =
        listOf(
            TurnoUi("Lun 26", "06:00–18:00", "Emergencias"),
            TurnoUi("Mié 28", "06:00–18:00", "Emergencias"),
        )

    val perfilDoctor =
        PerfilUi(
            nombre = "Dr. Jorge Peña",
            rolYTipo = "Doctor · Médico de emergencia",
            correo = "jorge.pena@hospital.edu",
            telefono = "7000-0000",
        )
}
