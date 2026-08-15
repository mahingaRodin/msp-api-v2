ALTER TABLE products ALTER COLUMN image TYPE VARCHAR(1024);

UPDATE products SET image = 'https://images.unsplash.com/photo-1563636619-e9143da7973b?auto=format&fit=crop&w=800&q=80'
WHERE sku = 'MILK-1L' AND (image IS NULL OR image = '');

UPDATE products SET image = 'https://images.unsplash.com/photo-1596040033229-a9821ebd058d?auto=format&fit=crop&w=800&q=80'
WHERE sku = 'AKABANGA-125' AND (image IS NULL OR image = '');

UPDATE products SET image = 'https://images.unsplash.com/photo-1629203851122-3726ecdf080e?auto=format&fit=crop&w=800&q=80'
WHERE sku = 'COKE-500' AND (image IS NULL OR image = '');

UPDATE products SET image = 'https://images.unsplash.com/photo-1564890369478-c89ca6d59b2f?auto=format&fit=crop&w=800&q=80'
WHERE sku = 'TEA-500' AND (image IS NULL OR image = '');

UPDATE products SET image = 'https://images.unsplash.com/photo-1584305574647-0cc949ae2d0e?auto=format&fit=crop&w=800&q=80'
WHERE sku = 'SOAP-MAR' AND (image IS NULL OR image = '');

UPDATE products SET image = 'https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&w=800&q=80'
WHERE image IS NULL OR image = '';

UPDATE products SET brand = 'POSify' WHERE brand = 'POSify Demo';
UPDATE products SET description = name WHERE description = 'Demo product';
