-- Completa audit_config con las entidad/accion que ya cuentan con @Auditable en el código
-- pero que nunca recibieron fila de configuración (AuditService.registrar() descarta el
-- evento en silencio si no existe fila activa en audit_config para ese par entidad/accion).
-- Sin esta migración, TODAS estas auditorías quedan inertes en tiempo de ejecución.

INSERT INTO public.audit_config (entidad, accion, activo, notificar_email, severidad, descripcion) VALUES
-- Decisiones de reglamento de alto impacto
('ProyectoTitulacion',        'CAMBIAR_DIRECTOR',              true, true,  'HIGH',     'Cambio de director del trabajo de titulación'),
('TribunalProyecto',          'ASIGNAR_O_REEMPLAZAR',          true, true,  'HIGH',     'Asignación o reemplazo de miembro del tribunal'),
('MatriculaTitulacion',       'CAMBIAR_OPCION',                true, true,  'HIGH',     'Cambio de opción de titulación'),
('ExamenComplexivoAplicacion','FRAUDE',                        true, true,  'CRITICAL', 'Registro de fraude académico en examen complexivo'),
('AnteproyectoTitulacion',    'SOLICITAR_MODIFICACION',        true, false, 'MEDIUM',   'Solicitud de modificación de anteproyecto'),
('AnteproyectoTitulacion',    'APROBAR_MODIFICACION',          true, true,  'HIGH',     'Aprobación de modificación de tema/título de anteproyecto'),
('PropuestaTitulacion',       'APROBAR_O_RECHAZAR',            true, true,  'HIGH',     'Aprobación o rechazo de propuesta de titulación'),
('PropuestaTitulacion',       'APROBAR_O_RECHAZAR_COMPLEXIVO', true, true,  'HIGH',     'Aprobación o rechazo de propuesta de examen complexivo'),
('PropuestaTitulacion',       'APROBAR_Y_DESIGNAR',            true, true,  'HIGH',     'Aprobación de propuesta con designación de director/tribunal'),
('BancoTemas',                'APROBAR_CONSEJO_DIRECTIVO',     true, true,  'HIGH',     'Aprobación de tema por Consejo Directivo'),

-- Banco de temas / propuestas (rutina)
('BancoTemas',                'CREATE',                        true, false, 'LOW',      'Registro de tema en banco de temas'),
('BancoTemas',                'SUGERIR',                       true, false, 'LOW',      'Sugerencia de tema por estudiante'),
('BancoTemas',                'APROBAR_SUGERENCIA',            true, false, 'MEDIUM',   'Aprobación de tema sugerido por estudiante'),
('BancoTemas',                'RECHAZAR_SUGERENCIA',           true, false, 'MEDIUM',   'Rechazo de tema sugerido por estudiante'),

-- Documento TIC
('DocumentoTIC',              'CREATE',                        true, false, 'LOW',      'Creación de documento TIC'),
('SeccionDocumentoTIC',       'CREAR_PERSONALIZADA',           true, false, 'LOW',      'Creación de sección personalizada de documento TIC'),
('VersionDocumentoTIC',       'CREAR',                         true, false, 'LOW',      'Nueva versión de documento TIC'),
('ComentarioDocumentoTIC',    'CREATE',                        true, false, 'LOW',      'Comentario en documento TIC'),

-- Examen complexivo
('ExamenComplexivoAplicacion','PROGRAMAR',                     true, false, 'MEDIUM',   'Programación de examen complexivo'),
('ExamenComplexivoAplicacion','REGISTRAR_INGRESO',             true, false, 'LOW',      'Registro de ingreso a examen complexivo'),
('ExamenComplexivoAplicacion','INICIAR',                       true, false, 'LOW',      'Inicio de examen complexivo'),
('ExamenComplexivoAplicacion','FINALIZAR',                     true, false, 'MEDIUM',   'Finalización y calificación de examen complexivo'),
('ExamenComplexivoAplicacion','ASIGNAR_DELEGADO',              true, false, 'MEDIUM',   'Asignación de docente delegado en examen complexivo'),

-- Matrícula / oportunidades / justificaciones
('MatriculaTitulacion',       'CREATE',                        true, false, 'LOW',      'Matrícula en titulación'),
('EleccionTitulacion',        'SELECCIONAR_MODALIDAD',         true, false, 'MEDIUM',   'Selección de modalidad/opción de titulación'),
('RegistroOportunidad',       'REGISTRAR',                     true, false, 'MEDIUM',   'Registro de oportunidad de titulación'),
('AutorizacionTerceraMatricula','AUTORIZAR',                   true, true,  'HIGH',     'Autorización de tercera matrícula'),
('SolicitudJustificacion',    'SOLICITAR',                     true, false, 'LOW',      'Solicitud de justificación'),
('SolicitudJustificacion',    'RESOLVER',                      true, false, 'MEDIUM',   'Resolución de solicitud de justificación'),

-- Expediente digital
('ExpedienteEstudiante',      'CREATE',                        true, false, 'LOW',      'Creación de expediente de estudiante'),
('DocumentoExpediente',       'CARGAR',                        true, false, 'LOW',      'Carga de documento habilitante en expediente'),
('DocumentoExpediente',       'VALIDAR',                       true, false, 'MEDIUM',   'Validación de documento de expediente'),
('DocumentoExpediente',       'RECHAZAR',                      true, false, 'MEDIUM',   'Rechazo de documento de expediente'),
('DocumentoExpediente',       'REGISTRAR_GENERADO',            true, false, 'LOW',      'Registro de documento generado en expediente'),

-- Grado / título / incorporación
('ActaGrado',                 'GENERAR_INCORPORACION',         true, false, 'MEDIUM',   'Generación de acta de incorporación'),
('ActaGrado',                 'FIRMAR',                        true, true,  'HIGH',     'Firma de acta de grado'),
('ActaGrado',                 'PROGRAMAR_CEREMONIA',           true, false, 'LOW',      'Programación de ceremonia de incorporación'),
('TituloProfesional',         'EMITIR',                        true, true,  'HIGH',     'Emisión de título profesional'),
('TituloProfesional',         'REGISTRAR_SENESCYT',            true, true,  'HIGH',     'Registro de título en SENESCYT'),

-- Banco de temas: eje temático / líneas de investigación
('EjeTematico',               'CREATE',                        true, false, 'LOW',      'Creación de eje temático'),
('EjeTematico',               'UPDATE',                        true, false, 'LOW',      'Actualización de eje temático'),
('EjeTematico',               'PLANIFICAR',                    true, false, 'LOW',      'Planificación de eje temático'),
('EjeTematico',               'APROBAR_PLANIFICACION',         true, false, 'MEDIUM',   'Aprobación de planificación de eje temático'),
('EjeTematico',               'DELETE',                        true, true,  'MEDIUM',   'Eliminación de eje temático'),
('LineaInvestigacion',        'CREATE',                        true, false, 'LOW',      'Creación de línea de investigación'),
('LineaInvestigacion',        'UPDATE',                        true, false, 'LOW',      'Actualización de línea de investigación'),
('LineaInvestigacion',        'DELETE',                        true, true,  'MEDIUM',   'Eliminación de línea de investigación'),

-- Antiplagio / actualización de conocimientos / bibliografía / preguntas
('AntiplacioIntento',         'REGISTRAR_RESULTADO_COMPILATIO',true, false, 'LOW',      'Registro de resultado de verificación antiplagio'),
('AntiplacioIntento',         'EMITIR_CERTIFICADO',            true, false, 'MEDIUM',   'Emisión de certificado antiplagio'),
('ActualizacionConocimientos','SOLICITAR',                     true, false, 'LOW',      'Solicitud de actualización de conocimientos'),
('ActualizacionConocimientos','RESOLVER',                      true, false, 'MEDIUM',   'Resolución de actualización de conocimientos'),
('ReferenciaBibliografica',   'CREATE',                        true, false, 'LOW',      'Registro de referencia bibliográfica'),
('ReferenciaBibliografica',   'UPDATE',                        true, false, 'LOW',      'Actualización de referencia bibliográfica'),
('PreguntaComplexivo',        'CREATE',                        true, false, 'LOW',      'Creación de pregunta de banco de complexivo'),
('PreguntaComplexivo',        'UPDATE',                        true, false, 'LOW',      'Actualización de pregunta de banco de complexivo'),
('PreguntaComplexivo',        'DESACTIVAR',                    true, false, 'LOW',      'Desactivación de pregunta de banco de complexivo'),

-- Configuración y asignaciones docentes
('Dt1Asignacion',             'UPDATE',                        true, false, 'MEDIUM',   'Modificación de asignación DT1'),
('Dt1Asignacion',             'CONFIGURAR_MODALIDAD_ENTREGA',  true, false, 'LOW',      'Configuración de modalidad de entrega DT1'),
('ConfiguracionCorreo',       'CREATE',                        true, true,  'HIGH',     'Creación de configuración de correo'),
('PeriodoTitulacion',         'DELETE',                        true, true,  'HIGH',     'Eliminación de período de titulación'),
('PeriodoTitulacion',         'AVAL_UNIDAD_REGISTRO',          true, false, 'MEDIUM',   'Aval de unidad de registro sobre calendario'),

-- SGA
('PensumImportado',           'IMPORTAR',                      true, false, 'LOW',      'Importación de pensum desde SGA'),
('RequisitoImportado',        'IMPORTAR',                      true, false, 'LOW',      'Importación de requisitos desde SGA'),
('DeudaImportada',            'IMPORTAR',                      true, false, 'LOW',      'Importación de deudas desde SGA')
ON CONFLICT (entidad, accion) DO NOTHING;
