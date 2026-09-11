package com.example.alertatemprana.modelos

import com.example.alertatemprana.R

data class GuiaCatastrofe(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val imagen: Int,
    val videoUrl: String,
    val pasos: List<String>
)

val guias = listOf(
    GuiaCatastrofe(
        id = "terremoto",
        titulo = "Terremoto",
        descripcion = "Un sismo o terremoto es un movimiento brusco de la tierra, causado por la liberación repentina de energía dentro de la misma tierra. En Argentina se producen por el contacto de la placa de Nazca con la placa Sudamericana, sobre todo en las provincias del oeste, aunque ninguna parte del país está exenta de este fenómeno.",
        imagen = R.drawable.terremoto,
        videoUrl = "https://www.youtube.com/embed/UUGT39JINoI",
        pasos = listOf(
            "Mantené la calma y ubicate en un lugar seguro, debajo de un elemento firme y si no es posible, junto a él.",
            "Cortá la energía eléctrica y cerrá las llaves de paso de agua y gas.",
            "Para iluminar, usá linternas. Velas, fósforos o encendedores pueden provocar explosiones en caso de fuga de gas.",
            "En la calle, mantenete alejado de edificios, postes y cables eléctricos.",
            "Si estás conduciendo, disminuí la velocidad. En lo posible, detenete en un lugar seguro.",
            "Protegéte la cabeza y el cuello con los brazos y esperá instrucciones de las autoridades.",
            "Si quedaste encerrado/a o atrapado/a, mantené la calma y solicitá auxilio."
        )
    ),
    GuiaCatastrofe(
        id = "inundacion",
        titulo = "Inundación",
        descripcion = "Una inundación se produce cuando hay un rápido crecimiento del nivel del agua que cubre o llena determinadas áreas. En la Argentina se producen por lluvias intensas, fuertes vientos, deshielo, desborde de represas o actividades humanas como la tala de árboles o la impermeabilización de suelos.",
        imagen = R.drawable.inundacion,
        videoUrl = "https://www.youtube.com/embed/0toqpF7tFN4",
        pasos = listOf(
            "Conservá la calma. Cerrá la llave de gas, agua y cortá la electricidad.",
            "Evacuá hacia una zona segura, que por lo general son las más altas.",
            "Si te sorprende el agua dentro de la vivienda, evitá sótanos y planta baja. Llamá a los Bomberos para que te ayuden a evacuar.",
            "Mantené cerradas puertas y ventanas, para evitar corrientes de agua dentro de la vivienda.",
            "No usés ningún tipo de vehículo como auto, moto o bicicleta.",
            "No te ubiques cerca de postes de electricidad o cables.",
            "Si hay heridos, llamá a las autoridades. No intentés moverlos.",
            "No intentés caminar o nadar por caminos inundados o cauces de río.",
            "Tomá agua potabilizada, no utilicés el agua de la canilla."
        )
    ),
    GuiaCatastrofe(
        id = "incendio",
        titulo = "Incendio forestal",
        descripcion = "Un incendio forestal es un fuego descontrolado de rápida propagación que afecta a bosques, llanuras, pastizales y pasturas, entre otras. El 95% de los incendios forestales son producidos por la mano del hombre: fogatas y colillas mal apagadas, abandono de tierras o preparación de áreas de pastoreo con fuego.",
        imagen = R.drawable.incendio_forestal,
        videoUrl = "https://www.youtube.com/embed/7M_7DP6EQJw",
        pasos = listOf(
            "Mantené las puertas y ventanas totalmente cerradas para evitar el ingreso del humo y de las chispas.",
            "Tratá que el suelo alrededor de la vivienda esté húmedo para evitar el avance del fuego.",
            "No salgas de tu casa a menos que el personal de bomberos o de las Fuerzas de Seguridad te lo indique, o que el riesgo de incendio de la vivienda sea inminente.",
            "Si la autoridad determina la evacuación, acatá las indicaciones. Procurá cubrirte boca y nariz con un paño, para no inhalar humo.",
            "No vuelvas a un área quemada. Los sitios calientes pueden reactivarse sin previo aviso.",
            "Nunca te sitúes en la parte alta de una montaña ni corras en sentido ascendente: el fuego avanza al subir hasta 17 veces más rápido que tú.",
            "Si la situación se torna peligrosa, acostate en el suelo y tratá de respirar a través de una prenda mojada.",
            "Si estás en una ruta y ves una columna de humo, avisá de inmediato a los bomberos."
        )
    )
)