# Dromos

**Le carnet de course et de marche qui reste dans ta poche.**

Dromos enregistre tes sorties à pied (marche et course) par GPS, les analyse et les garde sur ton
téléphone. Pas de compte, pas de cloud, pas de services Google : tes tracés t'appartiennent.

> Projet en cours de construction.

## Fonctionnalités prévues

- Suivi GPS en temps réel, même écran éteint : distance, durée, allure instantanée et moyenne,
  dénivelé positif et négatif, tracé sur carte.
- Pause manuelle et automatique, reprise, écran de synthèse en fin de sortie.
- Historique local, découpage au kilomètre, profils d'élévation et d'allure.
- Export et import GPX / KML.
- Carte OpenStreetMap vectorielle, utilisable sans réseau.

## Vie privée

- Aucune donnée ne quitte le téléphone sans action explicite de ta part (export, partage).
- Aucune télémétrie, aucune publicité, aucun compte.
- La localisation n'est utilisée que pendant une sortie, via une notification visible.

## Compiler

Prérequis : JDK 17 ou plus (le `jbr` d'Android Studio convient).

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

## Licence

[AGPL-3.0](LICENSE).
