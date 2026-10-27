import json
import urllib.parse
import boto3

s3 = boto3.client('s3')

def lambda_handler(event, context):
    # Pobierz nazwę koszyka i nazwę wgranego pliku z triggera S3
    bucket = event['Records'][0]['s3']['bucket']['name']
    key = urllib.parse.unquote_plus(event['Records'][0]['s3']['object']['key'], encoding='utf-8')
    size = event['Records'][0]['s3']['object'].get('size', 0)

    # Ignoruj pliki raportów, aby nie wywołać pętli nieskończonej
    if key.endswith('.meta.json'):
        return {'statusCode': 200, 'body': 'Pominięto plik metadanych'}

    print(f"LAMBDA: Wykryto nowy plik multimedialny w S3: {key} (Rozmiar: {size} bajtów)")

    # Automatyczne wygenerowanie pliku metadanych w S3
    metadata_content = {
        "originalFile": key,
        "sizeBytes": size,
        "processedBy": "AWS Lambda Multimedia Processor",
        "status": "VERIFIED"
    }

    meta_key = f"metadata/{key}.meta.json"
    s3.put_object(
        Bucket=bucket,
        Key=meta_key,
        Body=json.dumps(metadata_content),
        ContentType='application/json'
    )

    return {
        'statusCode': 200,
        'body': json.dumps(f'Pomyślnie przetworzono plik {key}')
    }