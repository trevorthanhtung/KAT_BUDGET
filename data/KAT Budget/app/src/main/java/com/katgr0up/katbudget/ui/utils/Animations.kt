package com.katgr0up.katbudget.ui.utils

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

val KatSpringSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium
)