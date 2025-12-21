const fs = require('fs');
const path = require('path');

// Algorithmic Art to Android Vector Drawable Converter
// Based on Financial Precision philosophy

class IconGenerator {
  constructor() {
    // Default parameters
    this.params = {
      complexity: 7,
      density: 0.6,
      rotation: 0,
      primaryHue: 210,
      saturation: 70,
      brightness: 65
    };

    // Icon-specific seeds for consistency
    this.iconSeeds = {
      dashboard: 1001,
      tradingplan: 2002,
      orders: 3003,
      personal: 4004
    };

    // Color palette based on Financial Precision
    this.colors = {};
  }

  // Simple pseudo-random number generator with seed
  seededRandom(seed) {
    const x = Math.sin(seed) * 10000;
    return x - Math.floor(x);
  }

  // Update color palette based on parameters
  updateColors() {
    const h = this.params.primaryHue;
    const s = this.params.saturation;
    const b = this.params.brightness;

    this.colors.primary = this.hsbToHex(h, s, b);
    this.colors.secondary = this.hsbToHex((h + 120) % 360, s, b);
    this.colors.accent = this.hsbToHex((h + 240) % 360, s, b);
  }

  // Convert HSB to hex color
  hsbToHex(h, s, b) {
    s /= 100;
    b /= 100;

    const k = (n) => (n + h / 60) % 6;
    const f = (n) => b * (1 - s * Math.max(0, Math.min(k(n), 4 - k(n), 1)));

    const r = Math.round(255 * f(5));
    const g = Math.round(255 * f(3));
    const bl = Math.round(255 * f(1));

    return ((r << 16) | (g << 8) | bl).toString(16).padStart(6, '0');
  }

  // Generate SVG path for dashboard icon
  generateDashboardIcon(isSelected = false) {
    const seed = this.iconSeeds.dashboard;
    const size = 24; // Android icon size
    const complexity = this.params.complexity;
    const paths = [];

    // Adjust colors for selected state - use Chinese Red (#DE2910) as primary
    const colors = isSelected ?
      { primary: '#DE2910', secondary: '#FF5722', accent: '#FF9800' } :
      { primary: '#888888', secondary: '#AAAAAA', accent: '#CCCCCC' };

    // Larger center point for better visibility
    paths.push({
      fill: colors.primary,
      d: `M12,12 m-3,0 a3,3 0 1,1 6,0 a3,3 0 1,1 -6,0`
    });

    // Outer circle with thicker stroke
    paths.push({
      fill: 'none',
      stroke: colors.primary,
      strokeWidth: 2,
      d: `M12,12 m-10,0 a10,10 0 1,1 20,0 a10,10 0 1,1 -20,0`
    });

    // Inner circle for better visual structure
    paths.push({
      fill: 'none',
      stroke: colors.secondary,
      strokeWidth: 1.5,
      d: `M12,12 m-6,0 a6,6 0 1,1 12,0 a6,6 0 1,1 -12,0`
    });

    // Data segments - thicker lines for better visibility
    const segments = complexity;
    for (let i = 0; i < segments; i++) {
      const angle = (i * 360 / segments) * Math.PI / 180;
      const r = 6 + this.seededRandom(seed + i) * 4; // Adjusted radius range
      const x = 12 + Math.cos(angle) * r;
      const y = 12 + Math.sin(angle) * r;

      paths.push({
        fill: 'none',
        stroke: i % 3 === 0 ? colors.primary : i % 3 === 1 ? colors.secondary : colors.accent,
        strokeWidth: 2, // Thicker lines
        d: `M12,12 L${x},${y}`
      });
    }

    // Arc segments
    for (let i = 0; i < segments; i++) {
      const startAngle = i * 360 / segments;
      const endAngle = (i + 1) * 360 / segments;
      const r = 5 + this.seededRandom(seed + i + 100) * 5;

      const x1 = 12 + Math.cos(startAngle * Math.PI / 180) * r;
      const y1 = 12 + Math.sin(startAngle * Math.PI / 180) * r;
      const x2 = 12 + Math.cos(endAngle * Math.PI / 180) * r;
      const y2 = 12 + Math.sin(endAngle * Math.PI / 180) * r;

      const largeArcFlag = endAngle - startAngle > 180 ? 1 : 0;

      paths.push({
        fill: 'none',
        stroke: i % 3 === 0 ? colors.primary : i % 3 === 1 ? colors.secondary : colors.accent,
        d: `M${x1},${y1} A${r},${r} 0 ${largeArcFlag},1 ${x2},${y2}`
      });
    }

    return paths;
  }

  // Generate SVG path for trading plan icon
  generateTradingPlanIcon(isSelected = false) {
    const seed = this.iconSeeds.tradingplan;
    const size = 24;
    const complexity = this.params.complexity;
    const paths = [];

    // Adjust colors for selected state - use Chinese Red (#DE2910) as primary
    const colors = isSelected ?
      { primary: '#DE2910', secondary: '#FF5722', accent: '#FF9800' } :
      { primary: '#888888', secondary: '#AAAAAA', accent: '#CCCCCC' };

    // Grid
    const gridSize = Math.floor(complexity);
    const cellSize = size / gridSize;

    // Draw grid lines
    for (let i = 0; i <= gridSize; i++) {
      const pos = i * cellSize;

      // Vertical lines
      paths.push({
        fill: 'none',
        stroke: colors.primary,
        d: `M${pos},0 L${pos},${size}`
      });

      // Horizontal lines
      paths.push({
        fill: 'none',
        stroke: colors.primary,
        d: `M0,${pos} L${size},${pos}`
      });
    }

    // Fill some cells
    for (let i = 0; i < gridSize * gridSize * this.params.density; i++) {
      const x = Math.floor(this.seededRandom(seed + i) * gridSize);
      const y = Math.floor(this.seededRandom(seed + i + 1000) * gridSize);

      const xPos = x * cellSize + cellSize / 2;
      const yPos = y * cellSize + cellSize / 2;
      const cellSize2 = cellSize * 0.6;

      paths.push({
        fill: this.seededRandom(seed + i + 2000) > 0.5 ? colors.primary : colors.secondary,
        d: `M${xPos - cellSize2 / 2},${yPos - cellSize2 / 2} h${cellSize2} v${cellSize2} h-${cellSize2} Z`
      });
    }

    // Diagonal trend line
    paths.push({
      fill: 'none',
      stroke: colors.accent,
      strokeWidth: 2,
      d: `M0,${size} L${size},0`
    });

    return paths;
  }

  // Generate SVG path for orders icon - representing trade execution/delivery receipts
  generateOrdersIcon(isSelected = false) {
    const seed = this.iconSeeds.orders;
    const size = 24;
    const complexity = this.params.complexity;
    const paths = [];

    // Adjust colors for selected state - use Chinese Red (#DE2910) as primary
    const colors = isSelected ?
      { primary: '#DE2910', secondary: '#FF5722', accent: '#FF9800' } :
      { primary: '#888888', secondary: '#AAAAAA', accent: '#CCCCCC' };

    // Create a more distinctive order icon with buy/sell arrows and price chart
    // Background circle for better visibility
    paths.push({
      fill: 'none',
      stroke: colors.primary,
      strokeWidth: 2,
      d: `M12,12 m-10,0 a10,10 0 1,1 20,0 a10,10 0 1,1 -20,0`
    });

    // Buy arrow (up) - representing buy orders
    paths.push({
      fill: colors.secondary,
      d: `M8,16 L8,10 L6,12 M8,10 L10,12` // Up arrow with thicker lines
    });

    // Sell arrow (down) - representing sell orders
    paths.push({
      fill: colors.accent,
      d: `M16,8 L16,14 L14,12 M16,14 L18,12` // Down arrow with thicker lines
    });

    // Price chart line - representing price movement
    const points = [];
    const pointCount = 5;
    for (let i = 0; i < pointCount; i++) {
      const x = 5 + i * 3.5;
      const y = 12 + Math.sin(i * 0.8 + seed) * 3;
      points.push(`${x},${y}`);
    }

    paths.push({
      fill: 'none',
      stroke: colors.primary,
      strokeWidth: 2,
      d: `M${points.join(' L ')}`
    });

    // Order book representation - horizontal lines
    for (let i = 0; i < 3; i++) {
      const y = 6 + i * 4;
      const width = 8 + this.seededRandom(seed + i) * 6;

      paths.push({
        fill: 'none',
        stroke: i % 2 === 0 ? colors.secondary : colors.accent,
        strokeWidth: 1.5,
        d: `M6,${y} h${width}`
      });
    }

    // Central circle representing order execution
    paths.push({
      fill: colors.primary,
      d: `M12,12 m-2,0 a2,2 0 1,1 4,0 a2,2 0 1,1 -4,0`
    });

    return paths;
  }

  // Generate SVG path for personal icon
  generatePersonalIcon(isSelected = false) {
    const seed = this.iconSeeds.personal;
    const size = 24;
    const complexity = this.params.complexity;
    const paths = [];

    // Adjust colors for selected state - use Chinese Red (#DE2910) as primary
    const colors = isSelected ?
      { primary: '#DE2910', secondary: '#FF5722', accent: '#FF9800' } :
      { primary: '#888888', secondary: '#AAAAAA', accent: '#CCCCCC' };

    // Create a person silhouette icon - head and shoulders
    // Head (circle)
    paths.push({
      fill: colors.primary,
      d: `M12,8 m-3,0 a3,3 0 1,1 6,0 a3,3 0 1,1 -6,0`
    });

    // Shoulders/Body (trapezoid shape)
    paths.push({
      fill: colors.primary,
      d: `M6,18 L6,14 Q6,12 8,12 L16,12 Q18,12 18,14 L18,18 Z`
    });

    // Add a background circle for better visibility
    paths.push({
      fill: 'none',
      stroke: colors.primary,
      strokeWidth: 2,
      d: `M12,12 m-10,0 a10,10 0 1,1 20,0 a10,10 0 1,1 -20,0`
    });

    // Add some decorative elements around the person
    // Small circles representing connections or features
    const positions = [
      { x: 6, y: 6 },  // Top left
      { x: 18, y: 6 }, // Top right
      { x: 6, y: 18 }, // Bottom left
      { x: 18, y: 18 } // Bottom right
    ];

    positions.forEach((pos, i) => {
      paths.push({
        fill: i % 2 === 0 ? colors.secondary : colors.accent,
        d: `M${pos.x},${pos.y} m-1.5,0 a1.5,1.5 0 1,1 3,0 a1.5,1.5 0 1,1 -3,0`
      });
    });

    // Add connecting lines from center to corners
    positions.forEach((pos) => {
      paths.push({
        fill: 'none',
        stroke: colors.secondary,
        strokeWidth: 1.5,
        d: `M12,12 L${pos.x},${pos.y}`
      });
    });

    return paths;
  }

  // Convert paths to Android Vector Drawable format
  pathsToVectorDrawable(name, paths, description) {
    let xml = `<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <!-- ${description} -->
    <!-- Generated with parameters: complexity=${this.params.complexity}, density=${this.params.density}, rotation=${this.params.rotation} -->
    <!-- Color scheme: hue=${this.params.primaryHue}, saturation=${this.params.saturation}%, brightness=${this.params.brightness}% -->
`;

    for (const path of paths) {
      xml += `    <path\n`;

      if (path.fill && path.fill !== 'none') {
        // Check if the color already has a # prefix
        const fillColor = path.fill.startsWith('#') ? path.fill : `#${path.fill}`;
        xml += `        android:fillColor="${fillColor}"\n`;
      }

      if (path.stroke && path.stroke !== 'none') {
        // Check if the color already has a # prefix
        const strokeColor = path.stroke.startsWith('#') ? path.stroke : `#${path.stroke}`;
        xml += `        android:strokeColor="${strokeColor}"\n`;
      }

      if (path.strokeWidth) {
        xml += `        android:strokeWidth="${path.strokeWidth}"\n`;
      }

      xml += `        android:pathData="${path.d}"\n`;
      xml += `        />\n`;
    }

    xml += `</vector>`;

    return xml;
  }

  // Generate all icons
  generateAllIcons() {
    this.updateColors();

    const icons = {
      dashboard: {
        name: 'ic_dashboard',
        description: 'Dashboard Icon - Data monitoring and visualization',
        paths: this.generateDashboardIcon()
      },
      dashboard_selected: {
        name: 'ic_dashboard_selected',
        description: 'Dashboard Icon (Selected) - Data monitoring and visualization',
        paths: this.generateDashboardIcon(true) // Selected state
      },
      trade_plans: {
        name: 'ic_trade_plans',
        description: 'Trade Plans Icon - Strategy management and planning',
        paths: this.generateTradingPlanIcon()
      },
      trade_plans_selected: {
        name: 'ic_trade_plans_selected',
        description: 'Trade Plans Icon (Selected) - Strategy management and planning',
        paths: this.generateTradingPlanIcon(true) // Selected state
      },
      trade_orders: {
        name: 'ic_trade_orders',
        description: 'Trade Orders Icon - Trade execution and delivery receipts',
        paths: this.generateOrdersIcon()
      },
      trade_orders_selected: {
        name: 'ic_trade_orders_selected',
        description: 'Trade Orders Icon (Selected) - Trade execution and delivery receipts',
        paths: this.generateOrdersIcon(true) // Selected state
      },
      my: {
        name: 'ic_my',
        description: 'My Icon - User management and profile',
        paths: this.generatePersonalIcon()
      },
      my_selected: {
        name: 'ic_my_selected',
        description: 'My Icon (Selected) - User management and profile',
        paths: this.generatePersonalIcon(true) // Selected state
      }
    };

    return icons;
  }

  // Save all icons to files
  saveAllIcons(outputDir) {
    if (!fs.existsSync(outputDir)) {
      fs.mkdirSync(outputDir, { recursive: true });
    }

    const icons = this.generateAllIcons();

    for (const [key, icon] of Object.entries(icons)) {
      const xml = this.pathsToVectorDrawable(icon.name, icon.paths, icon.description);
      const filePath = path.join(outputDir, `${icon.name}.xml`);
      fs.writeFileSync(filePath, xml);
      console.log(`Generated ${filePath}`);
    }

    // Also save the parameters used
    const paramsPath = path.join(outputDir, 'icon_parameters.json');
    fs.writeFileSync(paramsPath, JSON.stringify(this.params, null, 2));
    console.log(`Generated ${paramsPath}`);

    // Save the color palette
    const colorsPath = path.join(outputDir, 'color_palette.json');
    fs.writeFileSync(colorsPath, JSON.stringify(this.colors, null, 2));
    console.log(`Generated ${colorsPath}`);
  }

  // Update parameters
  updateParameters(newParams) {
    this.params = { ...this.params, ...newParams };
  }
}

// Command line interface
if (require.main === module) {
  const generator = new IconGenerator();

  // Parse command line arguments
  const args = process.argv.slice(2);
  const outputDir = args[0] || './generated_icons';

  // Check for custom parameters
  const paramsIndex = args.indexOf('--params');
  if (paramsIndex !== -1 && paramsIndex + 1 < args.length) {
    try {
      const customParams = JSON.parse(args[paramsIndex + 1]);
      generator.updateParameters(customParams);
    } catch (e) {
      console.error('Invalid parameters JSON:', e.message);
      process.exit(1);
    }
  }

  // Generate and save icons
  generator.saveAllIcons(outputDir);
  console.log(`Icons generated successfully in ${outputDir}`);
}

module.exports = IconGenerator;